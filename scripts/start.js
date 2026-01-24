const { exec, spawn } = require('child_process');
const os = require('os');
const util = require('util');
const fs = require('fs');
const path = require('path');

const execAsync = util.promisify(exec);

// --- Helper Functions ---
const Colors = {
    Reset: "\x1b[0m",
    Bright: "\x1b[1m",
    Dim: "\x1b[2m",
    Underscore: "\x1b[4m",
    Blink: "\x1b[5m",
    Reverse: "\x1b[7m",
    Hidden: "\x1b[8m",
    FgBlack: "\x1b[30m",
    FgRed: "\x1b[31m",
    FgGreen: "\x1b[32m",
    FgYellow: "\x1b[33m",
    FgBlue: "\x1b[34m",
    FgMagenta: "\x1b[35m",
    FgCyan: "\x1b[36m",
    FgWhite: "\x1b[37m",
};

function colorLog(color, message) {
    console.log(`${color}${message}${Colors.Reset}`);
}

async function runCommand(command, description, ignoreError = false) {
    if (description) {
        colorLog(Colors.FgCyan, `▶️  ${description}`);
    }
    try {
        const { stdout, stderr } = await execAsync(command);
        // if (stdout) console.log(stdout); // Too verbose for most things
        if (stderr && !stderr.includes("warning")) console.error(Colors.Dim, stderr, Colors.Reset);
        return { success: true, stdout };
    } catch (error) {
        if (!ignoreError) {
            colorLog(Colors.FgRed, `❌ Error executing: ${command}`);
            // console.error(error); // Keep clean unless necessary
        }
        return { success: false, error };
    }
}

function findAdb() {
    // 1. Check PATH
    try {
        require('child_process').execSync('adb --version', { stdio: 'ignore' });
        return 'adb';
    } catch (e) {
        // 2. Check ANDROID_HOME
        const androidHome = process.env.ANDROID_HOME;
        if (androidHome) {
            const adbPath = path.join(androidHome, 'platform-tools', 'adb');
            if (fs.existsSync(adbPath)) {
                return adbPath;
            }
        }
        return null;
    }
}

// --- Main Logic ---

async function startBackend() {
    console.log("");
    colorLog(Colors.FgMagenta, '┌──────────────────────────────────────────┐');
    colorLog(Colors.FgMagenta, '│          Backend Services                │');
    colorLog(Colors.FgMagenta, '└──────────────────────────────────────────┘');
    
    // Check if .env exists
    if (!fs.existsSync('.env')) {
        colorLog(Colors.FgYellow, '⚠️  No .env file found. Copying .env.example...');
        try {
            fs.copyFileSync('.env.example', '.env');
            colorLog(Colors.FgGreen, '✅ Created .env file.');
        } catch (e) {
            colorLog(Colors.FgRed, '❌ Failed to create .env file.');
        }
    }

    await runCommand('docker-compose down', 'Cleaning up existing services...');
    
    colorLog(Colors.FgCyan, '▶️  Starting Docker containers (PocketBase + Server)...');
    // We run this detached
    const child = spawn('just', ['up'], {
        detached: true,
        stdio: 'ignore' 
    });
    child.unref();
    
    colorLog(Colors.FgGreen, '✅ Backend services are starting in the background.');
}

async function launchDesktop() {
    console.log("");
    colorLog(Colors.FgBlue, '┌──────────────────────────────────────────┐');
    colorLog(Colors.FgBlue, '│          Desktop App                     │');
    colorLog(Colors.FgBlue, '└──────────────────────────────────────────┘');
    
    colorLog(Colors.FgCyan, '▶️  Launching JVM Desktop Client...');
    const child = spawn('just', ['desktop'], {
        detached: true,
        stdio: 'ignore'
    });
    child.unref();
    
    colorLog(Colors.FgGreen, '✅ Desktop app launched.');
}

async function launchWeb() {
    console.log("");
    colorLog(Colors.FgYellow, '┌──────────────────────────────────────────┐');
    colorLog(Colors.FgYellow, '│          Web App                         │');
    colorLog(Colors.FgYellow, '└──────────────────────────────────────────┘');
    
    colorLog(Colors.FgCyan, '▶️  Launching Web Client...');
    const child = spawn('just', ['web'], {
        detached: true,
        stdio: 'ignore'
    });
    child.unref();
    
    colorLog(Colors.FgGreen, '✅ Web app launched.');
}

async function launchAndroid() {
    console.log("");
    colorLog(Colors.FgGreen, '┌──────────────────────────────────────────┐');
    colorLog(Colors.FgGreen, '│          Android App                     │');
    colorLog(Colors.FgGreen, '└──────────────────────────────────────────┘');

    if (os.platform() === 'win32') {
        colorLog(Colors.FgYellow, '⚠️  Android launch on Windows script needs update.');
        return;
    }

    const adbPath = findAdb();
    if (!adbPath) {
        colorLog(Colors.FgYellow, '⚠️  adb not found in PATH or ANDROID_HOME.');
        colorLog(Colors.FgYellow, '   Skipping Android launch.');
        return;
    }

    try {
        const { stdout } = await execAsync(`${adbPath} devices`);
        // Output has "List of devices attached" line, so we need > 1 line check usually, 
        // but sometimes empty line at end.
        const lines = stdout.trim().split('\n');
        if (lines.length > 1) {
            colorLog(Colors.FgGreen, `🤖 Found ${lines.length - 1} Android device(s).`);
            colorLog(Colors.FgCyan, '▶️  Installing and launching Android app...');
            
            // We use 'just android' which uses gradle. 
            // Gradle usually finds SDK if ANDROID_HOME is set.
            const child = spawn('just', ['android'], {
                detached: true,
                stdio: 'ignore'
            });
            child.unref();
            colorLog(Colors.FgGreen, '✅ Android build & install triggered.');
        } else {
            colorLog(Colors.FgYellow, '⚠️  No Android device found (adb detected).');
        }
    } catch (e) {
        colorLog(Colors.FgRed, `❌ Error checking adb devices: ${e.message}`);
    }
}

async function launchIOS() {
    console.log("");
    colorLog(Colors.FgWhite, '┌──────────────────────────────────────────┐');
    colorLog(Colors.FgWhite, '│          iOS App                         │');
    colorLog(Colors.FgWhite, '└──────────────────────────────────────────┘');

    if (os.platform() !== 'darwin') {
        colorLog(Colors.Dim, '   (Skipping iOS - macOS only)');
        return;
    }

    // Check Java Home
    if (!process.env.JAVA_HOME) {
         colorLog(Colors.FgYellow, '⚠️  JAVA_HOME is not set. Xcode build might fail.');
    }

    try {
        const { stdout: devicesJson } = await execAsync('xcrun simctl list devices --json');
        const devices = JSON.parse(devicesJson).devices;
        let udid = Object.values(devices).flat().find(d => d.state === 'Booted' && d.isAvailable)?.udid;

        if (!udid) {
            colorLog(Colors.FgYellow, '🔍 No booted simulator found. Looking for default...');
            // Prefer iPhone 15 or 14
            udid = Object.values(devices).flat().find(d => d.name.includes('iPhone 15') && d.isAvailable)?.udid;
            if (!udid) {
                 udid = Object.values(devices).flat().find(d => d.name.includes('iPhone') && d.isAvailable)?.udid;
            }

            if (udid) {
                await runCommand(`xcrun simctl boot ${udid}`, `Booting simulator ${udid}...`);
            } else {
                colorLog(Colors.FgRed, '❌ No available iOS simulators found to boot.');
                return;
            }
        }

        colorLog(Colors.FgGreen, `📱 Using simulator UDID: ${udid}`);
        
        // Build & Install
        // We use spawn for build to inherit env vars better and keep it robust
        colorLog(Colors.FgCyan, '▶️  Building and installing iOS app (this may take a moment)...');
        
        try {
            // Using execAsync for build to await completion
             await execAsync(`xcodebuild -sdk iphonesimulator -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "platform=iOS Simulator,id=${udid}" install`);
             
             await runCommand(`xcrun simctl launch ${udid} love.bside.app`, '🚀 Launching App...');
             colorLog(Colors.FgGreen, '✅ iOS app launched.');
        } catch (buildError) {
             colorLog(Colors.FgRed, '❌ iOS Build/Install failed.');
             // Check if it's the gradle phase
             if (buildError.stderr && buildError.stderr.includes('PhaseScriptExecution')) {
                 colorLog(Colors.FgYellow, '   Tip: Try running "./gradlew :composeApp:embedAndSignAppleFrameworkForXcode" manually to debug.');
             }
             // console.error(buildError);
        }

    } catch (error) {
        colorLog(Colors.FgRed, '❌ An error occurred during iOS setup.');
        // console.error(error);
    }
}


async function main() {
    console.clear();
    console.log(Colors.FgCyan + `
  ██████╗       ███████╗██╗██████╗ ███████╗
  ██╔══██╗      ██╔════╝██║██╔══██╗██╔════╝
  ██████╔╝█████╗███████╗██║██║  ██║█████╗  
  ██╔══██╗╚════╝╚════██║██║██║  ██║██╔══╝  
  ██████╔╝      ███████║██║██████╔╝███████╗
  ╚═════╝       ╚══════╝╚═╝╚═════╝ ╚══════╝
    ` + Colors.Reset);
    colorLog(Colors.Bright, "   Universal App Launcher | v1.0.0");
    console.log("\n");

    const platform = os.platform();
    // colorLog(Colors.FgYellow, `   OS: ${platform} | Arch: ${os.arch()}`);

    const launchPromises = [];

    // Start Backend
    await startBackend();

    // Clients
    launchPromises.push(launchDesktop());
    launchPromises.push(launchWeb());

    if (platform === 'darwin') { // macOS
        launchPromises.push(launchAndroid());
        launchPromises.push(launchIOS());
    } else if (platform === 'linux') { // Linux
        launchPromises.push(launchAndroid());
    }

    await Promise.all(launchPromises);

    console.log("\n");
    colorLog(Colors.FgGreen, "🎉 Launch sequence complete! Check background tabs/windows.");
    console.log("\n");
}

main().catch(err => {
    colorLog(Colors.FgRed, 'A critical error occurred:');
    console.error(err);
});