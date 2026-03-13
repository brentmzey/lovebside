const ftp = require("basic-ftp")

async function cleanup() {
    const client = new ftp.Client()
    client.ftp.verbose = true

    const host = process.env.POCKETHOST_FTP_HOST || "ftp.pockethost.io"
    const user = process.env.POCKETHOST_FTP_USER
    const password = process.env.POCKETHOST_FTP_PASSWORD
    const instanceName = process.env.POCKETHOST_INSTANCE || "bside"

    if (!user || !password) {
        console.error("Error: POCKETHOST_FTP_USER and POCKETHOST_FTP_PASSWORD env vars must be set.")
        process.exit(1)
    }

    try {
        console.log(`Connecting to ${host}...`)
        await client.access({
            host: host,
            user: user,
            password: password,
            secure: true,
            secureOptions: { rejectUnauthorized: false }
        })

        console.log(`Navigating to instance '${instanceName}'...`)
        try {
            await client.cd(instanceName)
        } catch (e) {
            console.warn(`Could not cd to ${instanceName}. Assuming root is correct or instance folder not found.`)
        }

        const remoteDir = "pb_migrations"
        console.log(`Entering ${remoteDir}...`)
        await client.cd(remoteDir)

        const files = await client.list()

        // Remove .db, .d.ts, BUT ALSO any migration file we uploaded (starting with 2025)
        // because the instance is crashing, implying a deep conflict or version mismatch.
        // We need to get it back to a running state (with only its original 16xx/17xx migrations).

        const filesToRemove = files.filter(f =>
            f.name.endsWith(".db") ||
            f.name.endsWith(".d.ts") ||
            f.name.startsWith("2025") // Remove all our local migrations
        )

        if (filesToRemove.length > 0) {
            console.log("Found invalid/conflicting files to remove:", filesToRemove.map(f => f.name))
            for (const file of filesToRemove) {
                console.log(`Removing ${file.name}...`)
                await client.remove(file.name)
            }
            console.log("Cleanup complete.")
        } else {
            console.log("No invalid files found in pb_migrations.")
        }

    } catch (err) {
        console.error("Cleanup failed:", err)
        process.exit(1)
    }
    client.close()
}

cleanup()
