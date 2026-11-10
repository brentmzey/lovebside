const ftp = require("basic-ftp")
const { execSync } = require("child_process")

/**
 * PocketHost is retiring SFTP/FTP file access on 2027-02-06 in favor of the `pockethost` CLI over
 * HTTPS (see pockethost.io/blog/sftp-sunset). Prefer it whenever POCKETHOST_TOKEN is set (a personal
 * access token from the PocketHost dashboard, sourced only from ~/.zshrc or ~/.zshenv - never
 * hardcoded, never printed, never committed), and only fall back to the legacy FTP path below when
 * it isn't, so cleanup never silently breaks mid-transition. Mirrors the old filter exactly: any
 * `.db`/`.d.ts` file, or any local migration file (names starting with "2025"), gets removed.
 */
function cleanupViaCli(instanceName) {
    console.log("[PocketHost CLI] POCKETHOST_TOKEN found; cleaning up pb_migrations/ via `pockethost rm`.")
    const listing = execSync(`pockethost files pb_migrations -I ${instanceName} --json`, { encoding: "utf8" })
    const files = JSON.parse(listing)
    const filesToRemove = files
        .map(f => f.name || f.path)
        .filter(name => name.endsWith(".db") || name.endsWith(".d.ts") || name.startsWith("2025"))

    if (filesToRemove.length === 0) {
        console.log("No invalid files found in pb_migrations.")
        return
    }
    console.log("Found invalid/conflicting files to remove:", filesToRemove)
    for (const name of filesToRemove) {
        console.log(`Removing ${name}...`)
        execSync(`pockethost rm "pb_migrations/${name}" -I ${instanceName}`, { stdio: "inherit" })
    }
    console.log("Cleanup complete.")
}

async function cleanup() {
    const instanceName = process.env.POCKETHOST_INSTANCE || "bside"
    if (process.env.POCKETHOST_TOKEN) {
        cleanupViaCli(instanceName)
        return
    }
    console.warn(
        "[PocketHost FTP] POCKETHOST_TOKEN not set - falling back to legacy FTP, which PocketHost retires " +
        "2027-02-06. Run `pockethost login <token>` (token from the PocketHost dashboard) and add " +
        '`export POCKETHOST_TOKEN="ph_pat_..."` to ~/.zshrc to switch to the supported CLI path.'
    )

    const client = new ftp.Client()
    client.ftp.verbose = true

    const host = process.env.POCKETHOST_FTP_HOST || "ftp.pockethost.io"
    const user = process.env.POCKETHOST_FTP_USER
    const password = process.env.POCKETHOST_FTP_PASSWORD

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
