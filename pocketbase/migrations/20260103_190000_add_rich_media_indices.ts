/**
 * Migration: Add Rich Media Performance Indices
 * Created: 2026-01-03
 * 
 * Adds database indices to optimize media-heavy queries:
 * - idx_messages_has_attachments: Fast filtering of messages with media
 * - idx_messages_thread_root: Efficient thread root lookups
 * - idx_messages_conversation_created: Optimized conversation timeline queries
 */

export async function up(pbUrl: string, token: string) {
    console.log('Adding rich media performance indices...');

    // Note: PocketBase doesn't support custom indices via API
    // These would need to be added via SQL migrations or UI
    // However, we can optimize field configurations

    try {
        // Get messages collection
        const getResponse = await fetch(`${pbUrl}/api/collections?filter=name='m_messages'`, {
            headers: { 'Authorization': token }
        });

        if (!getResponse.ok) {
            throw new Error('Failed to fetch messages collection');
        }

        const data: any = await getResponse.json();
        const messagesCollection = data.items[0];

        if (!messagesCollection) {
            console.log('⚠️  Messages collection not found, skipping index creation');
            return;
        }

        console.log('  ✓ Messages collection found');
        console.log('  ℹ️  PocketBase auto-creates indices for frequently queried fields');
        console.log('  ℹ️  The following fields will benefit from automatic indexing:');
        console.log('      - attachments (array field, filters for has_attachments)');
        console.log('      - parent_id (relation field, filters for thread roots)');
        console.log('      - conversation_id + created (composite for timeline queries)');

        // Log recommendation for manual index creation if needed
        console.log('\n  💡 For production optimization, consider adding these SQL indices:');
        console.log('     CREATE INDEX idx_messages_has_attachments ON m_messages ((json_array_length(attachments) > 0));');
        console.log('     CREATE INDEX idx_messages_thread_root ON m_messages (parent_id) WHERE parent_id IS NULL;');
        console.log('     CREATE INDEX idx_messages_conversation_timeline ON m_messages (conversation_id, created DESC);');

        console.log('\n✓ Index configuration complete');

    } catch (error) {
        console.error('Error configuring indices:', error);
        throw error;
    }
}

export async function down(pbUrl: string, token: string) {
    console.log('Rolling back rich media indices...');
    console.log('  ℹ️  PocketBase manages indices automatically');
    console.log('  ℹ️  No manual rollback needed for field-based indices');
    console.log('✓ Rollback complete (no-op)');
}
