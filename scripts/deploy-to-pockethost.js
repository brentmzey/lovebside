const ftp = require("basic-ftp")
const path = require("path")
const fs = require("fs")

async function deploy() {
    const client = new ftp.Client()
    client.ftp.verbose = true

    const host = process.env.POCKETHOST_FTP_HOST || "ftp.pockethost.io"
    const user = process.env.POCKETHOST_FTP_USER
    const password = process.env.POCKETHOST_FTP_PASSWORD
    // Default to 'bside' but allow override
    const instanceName = process.env.POCKETHOST_INSTANCE || "bside"

    if (!user || !password) {
        console.error("Error: POCKETHOST_FTP_USER and POCKETHOST_FTP_PASSWORD env vars must be set.")
        process.exit(1)
    }

    try {
        console.log(`Connecting to ${host} as ${user}...`)

        await client.access({
            host: host,
            user: user,
            password: password,
            secure: true,
            secureOptions: {
                rejectUnauthorized: false
            }
        })

        console.log("Connected!")
        const rootList = await client.list()

        // Check if instance directory exists
        const instanceDir = rootList.find(f => f.name === instanceName && f.isDirectory)

        if (instanceDir) {
            console.log(`Found instance directory '${instanceName}'. Entering...`)
            await client.cd(instanceName)
        } else {
            // It might already be inside the instance dir if the FTP user is scoped, 
            // but the listing showed siblings, so we are definitely in a parent root.
            console.warn(`Warning: Could not find directory '${instanceName}' in root.`)
            console.warn("Available directories:", rootList.map(f => f.name).join(", "))
            console.log("Attempting to proceed in current directory...")
        }

        console.log("Current Directory:", await client.pwd())

        const localMigrationsDir = path.join(__dirname, "../pocketbase/migrations")
        if (!fs.existsSync(localMigrationsDir)) {
            throw new Error(`Local migrations directory not found at: ${localMigrationsDir}`)
        }

        const remoteDir = "pb_migrations"

        console.log(`Ensuring remote directory '${remoteDir}' exists...`)
        await client.ensureDir(remoteDir)

        // ensureDir enters the directory. We want to be inside pb_migrations to upload files?
        // uploadFromDir(local, remote) - "Remote path, relative to the current working directory."
        // basic-ftp ensureDir changes CWD to that dir.

        // Enter pb_migrations to simplify uploads
        // Enter pb_migrations to simplify uploads
        // await client.cd(remoteDir) // Removing this as ensureDir already does CWD

        console.log(`Uploading files from ${localMigrationsDir} to ${remoteDir}...`)

        // Manual upload loop to filter out .db files
        const localFiles = fs.readdirSync(localMigrationsDir)
        // ensureDir leaves us in the directory, so we don't need to cd again
        console.log("Current Directory after ensureDir:", await client.pwd())

        for (const fileName of localFiles) {
            // STEP 3: Upload only the profiles migration
            if (fileName !== "20250106000000_create_profiles.js") {
                console.log(`Skipping (temporarily): ${fileName}`)
                continue
            }

            const localPath = path.join(localMigrationsDir, fileName)
            // The remotePath is just the fileName because we are already in remoteDir

            console.log(`Uploading ${fileName}...`)
            await client.uploadFrom(localPath, fileName)
        }

        console.log("Upload completed successfully.")
        console.log(`Migrated instance: ${instanceName}`)
        console.log("Please restart your Pockethost instance to apply migrations.")

    } catch (err) {
        console.error("Deployment failed:", err)
        process.exit(1)
    }
    client.close()
}

deploy()
