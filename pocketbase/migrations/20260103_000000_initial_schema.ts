/**
 * Initial Schema Migration
 * Created: 2026-01-03
 * Source: Production snapshot (Jan 2025)
 * 
 * This migration creates the complete B-Side database schema including:
 * - User authentication (t_user)
 * - Messaging system (m_messages, m_conversations, m_conversation_participants, etc.)
 * - Matching system (m_matches)
 * - Profiles (s_profiles)
 * - Proust questionnaires
 * - System tables (pb_migrations, tenant properties, etc.)
 */

import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SCHEMA_PATH = path.join(__dirname, '..', 'schemas_archive', 'prod_snapshot_jan_2025.json');

export async function up(pbUrl: string, token: string) {
    console.log('Applying initial schema...');

    // Load production schema
    const schemaData = JSON.parse(fs.readFileSync(SCHEMA_PATH, 'utf8'));

    // Get existing collections to avoid duplicates/collisions
    const existingResp = await fetch(`${pbUrl}/api/collections?perPage=500`, {
        headers: { 'Authorization': token }
    });
    const existingItems = existingResp.ok ? (await existingResp.json()).items : [];
    const existingIds = new Map(existingItems.map((c: any) => [c.id, c]));
    const existingNames = new Set(existingItems.map((c: any) => c.name));

    let created = 0;
    let updated = 0;
    let skipped = 0;

    // Filter out system collections that shouldn't be touched via API unless they are being renamed/updated?
    // Actually, we should allow updating 'users' if it exists.
    const systemCollections = ['_superusers', '_authOrigins', '_externalAuths', '_mfas', '_otps'];

    // Sort collections by dependencies (weighted sort)
    const weights: Record<string, number> = {
        't_user': 0, // Auth first
        'm_conversations': 10,
        't_proust_questionnaire': 10,
        'm_messages': 15, // Messages before participants
        'm_conversation_participants': 20,
        'm_read_receipts': 30, // Depends on messages and participants
        't_user_questionnaire_responses': 30,
    };

    const sorted = schemaData
        .filter((col: any) => !systemCollections.includes(col.name))
        .sort((a: any, b: any) => {
            const weightA = weights[a.name] ?? 50;
            const weightB = weights[b.name] ?? 50;

            if (weightA !== weightB) {
                return weightA - weightB;
            }

            // Fallback to existing logic
            if (a.type === 'auth' && b.type !== 'auth') {
                return -1;
            }
            if (a.type !== 'auth' && b.type === 'auth') {
                return 1;
            }
            if (a.name.startsWith('t_') && !b.name.startsWith('t_')) {
                return -1;
            }
            if (!a.name.startsWith('t_') && b.name.startsWith('t_')) {
                return 1;
            }
            return a.name.localeCompare(b.name);
        });

    // Track available collection IDs to safely create relations
    const availableIds = new Set<string>(existingIds.keys());

    // PASS 1: Create/Update collections with SAFE fields (no forward/self relations)
    console.log('--- PASS 1: Creating/Updating with safe fields ---');
    for (const collection of sorted) {
        try {
            // Filter fields: Include only non-relations OR relations to available IDs
            const safeFields = collection.fields.filter((f: any) =>
                f.type !== 'relation' || (f.collectionId && availableIds.has(f.collectionId))
            );

            // If existing, update, else create
            const existingById = collection.id ? existingIds.get(collection.id) : null;

            if (existingById) {
                console.log(`  [Pass 1] Updating existing: ${existingById.name} -> ${collection.name}...`);
                const response = await fetch(`${pbUrl}/api/collections/${existingById.id}`, {
                    method: 'PATCH',
                    headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        name: collection.name,
                        type: collection.type,
                        fields: safeFields,
                        // Update rules/options too
                        listRule: collection.listRule,
                        viewRule: collection.viewRule,
                        createRule: collection.createRule,
                        updateRule: collection.updateRule,
                        deleteRule: collection.deleteRule,
                        ...(collection.type === 'auth' && {
                            authRule: collection.authRule,
                            oauth2: collection.oauth2,
                            passwordAuth: collection.passwordAuth,
                            mfa: collection.mfa,
                            otp: collection.otp,
                            authToken: collection.authToken,
                            passwordResetToken: collection.passwordResetToken,
                            emailChangeToken: collection.emailChangeToken,
                            verificationToken: collection.verificationToken,
                            fileToken: collection.fileToken,
                            authAlert: collection.authAlert,
                            verificationTemplate: collection.verificationTemplate,
                            resetPasswordTemplate: collection.resetPasswordTemplate,
                            confirmEmailChangeTemplate: collection.confirmEmailChangeTemplate
                        })
                    })
                });

                if (!response.ok) {
                    const error = await response.text();
                    console.error(`  ✗ Failed to update ${collection.name} in Pass 1: ${error}`);
                    continue;
                }

                updated++;
                console.log(`  ✓ Updated ${collection.name} in Pass 1`);
            } else if (!existingNames.has(collection.name)) {
                console.log(`  [Pass 1] Creating new: ${collection.name}...`);
                const res = await fetch(`${pbUrl}/api/collections`, {
                    method: 'POST',
                    headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        id: collection.id,
                        name: collection.name,
                        type: collection.type,
                        fields: safeFields,
                        listRule: collection.listRule,
                        viewRule: collection.viewRule,
                        createRule: collection.createRule,
                        updateRule: collection.updateRule,
                        deleteRule: collection.deleteRule,
                        ...(collection.type === 'auth' && {
                            authRule: collection.authRule,
                            oauth2: collection.oauth2,
                            passwordAuth: collection.passwordAuth,
                            mfa: collection.mfa,
                            otp: collection.otp,
                            authToken: collection.authToken,
                            passwordResetToken: collection.passwordResetToken,
                            emailChangeToken: collection.emailChangeToken,
                            verificationToken: collection.verificationToken,
                            fileToken: collection.fileToken,
                            authAlert: collection.authAlert,
                            verificationTemplate: collection.verificationTemplate,
                            resetPasswordTemplate: collection.resetPasswordTemplate,
                            confirmEmailChangeTemplate: collection.confirmEmailChangeTemplate
                        })
                    })
                });

                if (!res.ok) {
                    const err = await res.text();
                    console.error(`  ✗ Failed to create ${collection.name} in Pass 1: ${err}`);
                    // Continue to next, but this might break dependencies.
                    // However, we track availableIds, so subsequent won't try to use this one.
                } else {
                    created++;
                    availableIds.add(collection.id);
                    console.log(`  ✓ Created ${collection.name} in Pass 1`);
                }
            } else {
                console.log(`  [Pass 1] Skipping ${collection.name} (exists by name, no ID match)`);
                skipped++;
                // Assuming it exists and we can link to it? Not safe if ID mismatched.
                // If it exists by name, its ID should already be in existingIds and thus availableIds.
                // If it's not in existingIds, then it's a name collision with a new ID, which is problematic.
                // For now, we skip and rely on existingIds being accurate.
            }
        } catch (error) {
            console.error(`  ✗ Error in Pass 1 for ${collection.name}:`, error);
        }
    }

    // PASS 2: Update ALL collections with COMPLETE schema (relations and indexes)
    console.log('\n--- PASS 2: Updating with full schema (relations and indexes) ---');
    for (const collection of sorted) {
        // Only process collections that exist (either were existing or created in Pass 1)
        if (!availableIds.has(collection.id)) {
            console.log(`  [Pass 2] Skipping ${collection.name} (not available for full update)`);
            continue;
        }

        try {
            console.log(`  [Pass 2] Finalizing ${collection.name}...`);
            const res = await fetch(`${pbUrl}/api/collections/${collection.id}`, {
                method: 'PATCH', // Update to add missing fields/relations
                headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fields: collection.fields, // FULL fields
                    indexes: collection.indexes // Apply indexes now too
                })
            });

            if (!res.ok) {
                console.error(`  ✗ Failed to finalize ${collection.name}: ${await res.text()}`);
            } else {
                console.log(`  ✓ Finalized ${collection.name}`);
            }
        } catch (error) {
            console.error(`  ✗ Error in Pass 2 for ${collection.name}:`, error);
        }
    }

    console.log(`\n✓ Migration complete: ${created} created, ${updated} updated, ${skipped} skipped`);
}

export async function down(pbUrl: string, token: string) {
    console.log('Rolling back initial schema...');
    console.warn('⚠️  This will delete ALL collections and data!');

    // Get all non-system collections
    const response = await fetch(`${pbUrl}/api/collections?perPage=500`, {
        headers: { 'Authorization': token }
    });

    if (!response.ok) {
        console.error('Failed to fetch collections');
        return;
    }

    const data = await response.json();
    const systemCollections = ['_superusers', '_authOrigins', '_externalAuths', '_mfas', '_otps', 'pb_migrations'];
    const toDelete = data.items.filter((c: any) => !systemCollections.includes(c.name));

    for (const collection of toDelete) {
        try {
            console.log(`  Deleting: ${collection.name}...`);
            await fetch(`${pbUrl}/api/collections/${collection.id}`, {
                method: 'DELETE',
                headers: { 'Authorization': token }
            });
            console.log(`  ✓ Deleted ${collection.name}`);
        } catch (error) {
            console.error(`  ✗ Failed to delete ${collection.name}:`, error);
        }
    }

    console.log('✓ Rollback complete');
}
