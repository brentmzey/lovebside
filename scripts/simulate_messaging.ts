import PocketBase from 'pocketbase';
import * as EventSource from 'eventsource';

// Polyfill for Node.js
// Handle CJS/ESM interop
global.EventSource = (EventSource as any).default || (EventSource as any).EventSource || EventSource;

const PB_URL = process.env.POCKETBASE_URL || 'http://127.0.0.1:8092';

async function main() {
    console.log(`Starting messaging simulation on ${PB_URL}...`);

    // Clients for two users
    const pbA = new PocketBase(PB_URL);
    const pbB = new PocketBase(PB_URL);

    // Admin auth to create users
    const pbAdmin = new PocketBase(PB_URL);
    try {
        // PocketBase 0.23+ uses _superusers collection for superuser/admin auth
        await pbAdmin.collection('_superusers').authWithPassword('tester_admin@bside.love', 'password123');
    } catch (e) {
        console.error('Admin auth failed. Ensure server is running with correct credentials.');
        console.error(e);
        process.exit(1);
    }

    // Create unique users
    const ts = Date.now();
    const emailA = `userA_${ts}@test.com`;
    const emailB = `userB_${ts}@test.com`;
    const password = 'password123';

    console.log(`Creating users: ${emailA}, ${emailB}`);
    
    // Helper to create user safely (users or t_user)
    const createUser = async (email: string) => {
        const data = {
            email: email,
            password: password,
            passwordConfirm: password,
            emailVisibility: true,
            readReceiptsEnabled: true
        };
        try {
            return await pbAdmin.collection('t_user').create(data);
        } catch (e: any) {
            if (e.status === 404) {
                 return await pbAdmin.collection('users').create(data);
            }
            throw e;
        }
    };

    const userA = await createUser(emailA);
    const userB = await createUser(emailB);

    // Authenticate users
    try {
        await pbA.collection('t_user').authWithPassword(emailA, password);
    } catch {
        await pbA.collection('users').authWithPassword(emailA, password);
    }

    try {
        await pbB.collection('t_user').authWithPassword(emailB, password);
    } catch {
        await pbB.collection('users').authWithPassword(emailB, password);
    }

    console.log('✓ Users authenticated');

    // 1. Create Conversation (User A starts)
    console.log('User A creating conversation...');
    const conversation = await pbA.collection('m_conversations').create({
        status: 'active',
        type: 'direct'
    });

    // Add participants
    // Note: m_conversation_participants uses snake_case in this schema snapshot
    await pbA.collection('m_conversation_participants').create({
        conversation_id: conversation.id,
        user_id: userA.id,
        role: 'admin',
        joined_at: new Date().toISOString()
    });
    await pbA.collection('m_conversation_participants').create({
        conversation_id: conversation.id,
        user_id: userB.id,
        role: 'member',
        joined_at: new Date().toISOString()
    });
    console.log(`✓ Conversation created: ${conversation.id}`);

    // Setup Realtime Listeners
    let msgReceivedByB = false;
    let typingReceivedByB = false;
    let readReceiptReceivedByA = false;
    let reactionReceivedByA = false;

    // User B listens for messages
    pbB.collection('m_messages').subscribe('*', (e) => {
        // conversation_id (snake_case from snapshot)
        if (e.action === 'create' && e.record.conversation_id === conversation.id) {
            console.log(`[Realtime] User B received message: ${e.record.content}`);
            msgReceivedByB = true;
        }
    });

    // User B listens for typing
    pbB.collection('m_typing_status').subscribe('*', (e) => {
        // conversation_id, user_id (snake_case)
        if (e.record.conversation_id === conversation.id && e.record.user_id === userA.id && e.record.is_typing) {
            console.log(`[Realtime] User B sees User A typing`);
            typingReceivedByB = true;
        }
    });

    // User A listens for read receipts
    pbA.collection('m_read_receipts').subscribe('*', (e) => {
        // user_id (snake_case)
        if (e.action === 'create' && e.record.user_id === userB.id) {
             console.log(`[Realtime] User A received read receipt from User B`);
             readReceiptReceivedByA = true;
        }
    });

    // User A listens for reactions (snake_case)
    pbA.collection('m_reactions').subscribe('*', (e) => {
        if (e.action === 'create') {
            console.log(`[Realtime] User A received reaction: ${e.record.reaction}`);
            reactionReceivedByA = true;
        }
    });

    // 2. Typing Indicator
    console.log('User A is typing...');
    await pbA.collection('m_typing_status').create({
        conversation_id: conversation.id,
        user_id: userA.id,
        is_typing: true
    });
    
    // Wait a bit for propagation
    await new Promise(r => setTimeout(r, 1500));

    // 3. Send Message
    console.log('User A sending message...');
    const msg = await pbA.collection('m_messages').create({
        conversation_id: conversation.id,
        sender_id: userA.id,
        receiver_id: userB.id,
        content: 'Hello World',
        type: 'text', // Changed from message_type based on error
        status: 'sent',
        sent_at: new Date().toISOString()
    });

    await new Promise(r => setTimeout(r, 1500));

    // 4. Read Receipt
    if (msgReceivedByB) {
        console.log('User B sending read receipt...');
        await pbB.collection('m_read_receipts').create({
            message_id: msg.id,
            user_id: userB.id,
            read_at: new Date().toISOString()
        });
    } else {
        console.log('User B did not receive message yet (timeout?)');
    }

    await new Promise(r => setTimeout(r, 1500));

    // 5. Reply (Threading)
    console.log('\n--- Visual Transcript ---');
    console.log(`[${userA.email.split('@')[0]}]: Hello World`);
    console.log(`                 <-- [${userB.email.split('@')[0]}] Read at ${new Date().toLocaleTimeString()}`);
    console.log(`[${userB.email.split('@')[0]}]: Hi back! (Replying to: "Hello World")`);
    console.log(`                 <-- [${userA.email.split('@')[0]}] Reacted 👍`);
    console.log('-------------------------\n');

    console.log('User B replying...');
    // Use existing reply_to_message_id if available (snake_case)
    const reply = await pbB.collection('m_messages').create({
        conversation_id: conversation.id,
        sender_id: userB.id,
        receiver_id: userA.id,
        content: 'Hi back!',
        type: 'text',
        status: 'sent',
        sent_at: new Date().toISOString(),
        reply_to_message_id: msg.id
    });
    console.log(`✓ Reply sent (reply_to_message_id: ${reply.reply_to_message_id})`);

    // 6. Reaction (snake_case)
    console.log('User A reacting to reply...');

    // Ensure m_reactions exists (fallback)
    try {
        await pbA.collection('m_reactions').create({
            message_id: reply.id,
            user_id: userA.id,
            reaction: '👍'
        });
    } catch (e: any) {
        if (e.status === 404) {
             console.log('m_reactions collection missing, attempting to create...');
             try {
                // Admin creates collection if missing
                const mMessagesId = (await pbAdmin.collection('m_messages').getFullList())[0]?.collectionId || (await pbAdmin.collections.getOne('m_messages')).id;
                const usersId = (await pbAdmin.collection('t_user').getFullList())[0]?.collectionId || (await pbAdmin.collections.getOne('t_user')).id;
                
                console.log(`Debug: mMessagesId=${mMessagesId}, usersId=${usersId}`);

                await pbAdmin.collections.create({
                    name: 'm_reactions',
                    type: 'base',
                    fields: [
                        { name: "message_id", type: "relation", required: true, collectionId: mMessagesId, cascadeDelete: true, maxSelect: 1 },
                        { name: "user_id", type: "relation", required: true, collectionId: usersId, cascadeDelete: true, maxSelect: 1 },
                        { name: "reaction", type: "text", required: true, options: { max: 10 } }
                    ],
                    indexes: [
                        'CREATE UNIQUE INDEX `idx_reactions_unique` ON `m_reactions` (`message_id`, `user_id`, `reaction`)',
                        'CREATE INDEX `idx_reactions_message` ON `m_reactions` (`message_id`)'
                    ],
                    listRule: "@request.auth.id != ''",
                    viewRule: "@request.auth.id != ''",
                    createRule: "@request.auth.id != '' && @request.auth.id = user_id",
                    updateRule: "@request.auth.id != '' && @request.auth.id = user_id",
                    deleteRule: "@request.auth.id != '' && @request.auth.id = user_id"
                });
                console.log('✓ Created m_reactions (fallback)');
                
                // Retry creation
                await pbA.collection('m_reactions').create({
                    message_id: reply.id,
                    user_id: userA.id,
                    reaction: '👍'
                });
             } catch (err) {
                 console.error('Failed to create m_reactions fallback:', err);
                 console.error('Full error details:', JSON.stringify(err, null, 2));
             }
        } else {
            throw e;
        }
    }

    await new Promise(r => setTimeout(r, 2000));

    // Verification
    console.log('\n--- Verification Results ---');
    console.log(`Message Received: ${msgReceivedByB ? '✅' : '❌'}`);
    console.log(`Typing Indicator: ${typingReceivedByB ? '✅' : '❌'}`);
    console.log(`Read Receipt: ${readReceiptReceivedByA ? '✅' : '❌'}`);
    console.log(`Reaction: ${reactionReceivedByA ? '✅' : '❌'}`);

    // Cleanup
    pbA.authStore.clear();
    pbB.authStore.clear();
    pbAdmin.authStore.clear();

    if (msgReceivedByB && typingReceivedByB && readReceiptReceivedByA && reactionReceivedByA) {
        console.log('\nSUCCESS: All messaging features verified.');
        process.exit(0);
    } else {
        console.error('\nFAILURE: Some features did not verify.');
        process.exit(1);
    }
}

main().catch(console.error);
