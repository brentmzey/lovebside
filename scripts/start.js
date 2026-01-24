const { exec } = require('child_process');
const os = require('os');
const util = require('util');

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
    console.log(color, message, Colors.Reset);
}

async function runCommand(command, description) {
    if (description) {
        colorLog(Colors.FgCyan, `▶️  ${description}`);
    }
    try {
        const { stdout, stderr } = await execAsync(command);
        if (stdout) console.log(stdout);
        if (stderr) console.error(stderr);
        return { success: true };
    } catch (error) {
        colorLog(Colors.FgRed, `❌ Error executing: ${command}`);
        console.error(error);
        return { success: false };
    }
}

// --- Main Logic ---

async function startBackend() {
    colorLog(Colors.FgGreen, '--- Starting Backend ---');
    await runCommand('docker-compose down', 'Shutting down existing services...');
    // We run this in the background as it's a long-running process
    exec('just up');
    colorLog(Colors.FgGreen, '✅ Backend services are starting in the background.');
}

async function launchDesktop() {
    colorLog(Colors.FgGreen, '--- Launching Desktop ---');
    exec('just desktop');
    colorLog(Colors.FgGreen, '✅ Desktop app launched in the background.');
}

async function launchWeb() {
    colorLog(Colors.FgGreen, '--- Launching Web ---');
    exec('just web');
    colorLog(Colors.FgGreen, '✅ Web app launched in the background.');
}

async function launchAndroid() {
    colorLog(Colors.FgGreen, '--- Checking for Android ---');
    if (os.platform() === 'win32') {
        colorLog(Colors.FgYellow, 'Android launch on Windows is not yet supported by this script.');
        return;
    }
    const { stdout } = await execAsync('adb devices');
    if (stdout.split('\n').length > 2) {
        colorLog(Colors.FgGreen, '🤖 Found Android device. Installing and launching app...');
        await runCommand('just android', 'Installing Android app...');
    } else {
        colorLog(Colors.FgYellow, '⚠️  No Android device found. Please start an emulator or connect a device.');
    }
}

async function launchIOS() {
    colorLog(Colors.FgGreen, '--- Checking for iOS ---');
    if (os.platform() !== 'darwin') {
        colorLog(Colors.FgYellow, 'iOS development is only supported on macOS.');
        return;
    }

    try {
        const { stdout: devicesJson } = await execAsync('xcrun simctl list devices --json');
        const devices = JSON.parse(devicesJson).devices;
        let udid = Object.values(devices).flat().find(d => d.state === 'Booted' && d.isAvailable)?.udid;

        if (!udid) {
            colorLog(Colors.FgYellow, 'No booted simulator found. Trying to boot a default one...');
            udid = Object.values(devices).flat().find(d => d.name.includes('iPhone') && d.isAvailable)?.udid;
            if (udid) {
                await runCommand(`xcrun simctl boot ${udid}`, `Booting simulator ${udid}...`);
            } else {
                colorLog(Colors.FgRed, '❌ No available iOS simulators found.');
                return;
            }
        }

        colorLog(Colors.FgGreen, `📱 Using simulator with UDID: ${udid}`);
        await runCommand(`xcodebuild -sdk iphonesimulator -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "platform=iOS Simulator,id=${udid}" install`, 'Building and installing iOS app...');
        await runCommand(`xcrun simctl launch ${udid} love.bside.app`, '🚀 Launching iOS App...');
        colorLog(Colors.FgGreen, '✅ iOS app launched.');

    } catch (error) {
        colorLog(Colors.FgRed, '❌ An error occurred during iOS launch.');
        console.error(error);
    }
}


async function main() {
    console.log(`
  ____  ____  _     _      
 | __ )/ ___|(_) __| | ___ 
 |  _ \___ \| | (_| |  __ \
 | |_) |___) | | (_| |  __)/
 |____/|____/|_|\__,_|\___|
    `);
    colorLog(Colors.Bright, "🚀 Starting B-Side Universal Launcher...");
    console.log("");

    const platform = os.platform();
    colorLog(Colors.FgYellow, `Detected OS: ${platform}`);
    console.log("");

    const launchPromises = [];

    // --- Start all components ---
    // We start the backend first and let it run in the background.
    await startBackend();

    // Launch clients in parallel
    launchPromises.push(launchDesktop());
    launchPromises.push(launchWeb());

    if (platform === 'darwin') { // macOS
        launchPromises.push(launchAndroid());
        launchPromises.push(launchIOS());
    } else if (platform === 'linux') { // Linux
        launchPromises.push(launchAndroid());
    }

    await Promise.all(launchPromises);

    console.log("\n\n");
    colorLog(Colors.Bright, "🎉 All available targets have been launched! The backend is running in the background.");
}

main().catch(err => {
    colorLog(Colors.FgRed, 'A critical error occurred:');
    console.error(err);
});
