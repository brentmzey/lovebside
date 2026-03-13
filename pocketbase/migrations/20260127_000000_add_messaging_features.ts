/**
 * Add Messaging Features (Reactions, Read Receipts, Typing, Threading)
 * Created: 2026-01-27
 */

export async function up(pbUrl: string, token: string) {
    console.log('Applying messaging features migration...');

    // 0. Update users collection for read receipts preference
    try {
        let usersCol;
        try {
            const res = await fetch(`${pbUrl}/api/collections/users`, { headers: { 'Authorization': token } });
            if (res.ok) {
                usersCol = await res.json();
            }
        } catch (e) {}

        if (!usersCol) {
            try {
                const res = await fetch(`${pbUrl}/api/collections/t_user`, { headers: { 'Authorization': token } });
                if (res.ok) {
                    usersCol = await res.json();
                }
            } catch (e) {}
        }

        if (usersCol && usersCol.fields) {
            if (!usersCol.fields.find((f: any) => f.name === 'readReceiptsEnabled')) {
                console.log(`Adding readReceiptsEnabled to ${usersCol.name}...`);
                usersCol.fields.push({
                    "name": "readReceiptsEnabled",
                    "type": "bool",
                    "required": false,
                    "options": {}
                });
                await fetch(`${pbUrl}/api/collections/${usersCol.id}`, {
                    method: 'PATCH',
                    headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                    body: JSON.stringify({ fields: usersCol.fields })
                });
                console.log(`✓ Updated ${usersCol.name}`);
            }
        } else {
             console.error('Could not find users or t_user collection to update.');
        }
    } catch (e) { console.error('Error updating users:', e); }

    // 1. Update m_messages: Add replyToId and optimized media indices
    try {
        const msgCollection = await (await fetch(`${pbUrl}/api/collections/m_messages`, { headers: { 'Authorization': token } })).json();

        // Add attachments field if missing (for media support)
        if (!msgCollection.fields.find((f: any) => f.name === 'attachments')) {
            console.log('Adding attachments to m_messages...');
            msgCollection.fields.push({
                "name": "attachments",
                "type": "file",
                "required": false,
                "options": { "maxSelect": 10, "maxSize": 10485760 }
            });
        }

        // Check if replyToId exists (or reply_to_message_id)
        if (!msgCollection.fields.find((f: any) => f.name === 'replyToId') && !msgCollection.fields.find((f: any) => f.name === 'reply_to_message_id')) {
            console.log('Adding replyToId to m_messages...');
            msgCollection.fields.push({
                "name": "replyToId",
                "type": "relation",
                "required": false,
                "presentable": false,
                "unique": false,
                "options": {
                    "collectionId": msgCollection.id,
                    "cascadeDelete": false,
                    "minSelect": null,
                    "maxSelect": 1,
                    "displayFields": null
                }
            });
        }

        // Optimized indices
        const requiredIndices = [
            'CREATE INDEX `idx_messages_conversation_created` ON `m_messages` (`conversationId`, `created` DESC)',
            'CREATE INDEX `idx_m_msg_media` ON `m_messages` (`messageType`) WHERE `messageType` != "text"'
        ];
        // Only add replyToId index if we added the field or use existing one
        if (msgCollection.fields.find((f: any) => f.name === 'replyToId')) {
             requiredIndices.push('CREATE INDEX `idx_m_msg_reply` ON `m_messages` (`replyToId`)');
        }

        let indicesChanged = false;
        requiredIndices.forEach(idx => {
            if (!msgCollection.indexes.includes(idx)) {
                msgCollection.indexes.push(idx);
                indicesChanged = true;
            }
        });

        if (indicesChanged || (!msgCollection.fields.find((f: any) => f.name === 'replyToId') && !msgCollection.fields.find((f: any) => f.name === 'reply_to_message_id'))) {
             await fetch(`${pbUrl}/api/collections/${msgCollection.id}`, {
                method: 'PATCH',
                headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    fields: msgCollection.fields,
                    indexes: msgCollection.indexes
                })
            });
            console.log('✓ Updated m_messages');
        }

    } catch (e) {
        console.error('Error updating m_messages:', e);
    }

    // Helper to get collection ID safely
    const getId = async (name: string) => {
        const res = await fetch(`${pbUrl}/api/collections/${name}`, { headers: { 'Authorization': token } });
        if (!res.ok) {
            console.log(`getId('${name}') failed: ${res.status} ${res.statusText}`);
            return null;
        }
        const data = await res.json();
        console.log(`getId('${name}') -> ${data.id}`);
        return data.id;
    };

    // Helper to generate random ID (15 chars)
    const randomId = () => {
        let result = '';
        const characters = 'abcdefghijklmnopqrstuvwxyz0123456789';
        for (let i = 0; i < 15; i++) {
            result += characters.charAt(Math.floor(Math.random() * characters.length));
        }
        return result;
    };

    // 2. Create m_reactions
    try {
        const check = await fetch(`${pbUrl}/api/collections/m_reactions`, { headers: { 'Authorization': token } });
        if (check.status === 404) {
             console.log('Creating m_reactions...');
             
             // Inspect m_messages to see how relation fields are structured
             const msgColRes = await fetch(`${pbUrl}/api/collections/m_messages`, { headers: { 'Authorization': token } });
             if (msgColRes.ok) {
                 const msgCol = await msgColRes.json();
                 const relationField = msgCol.fields.find((f: any) => f.type === 'relation');
                 if (relationField) {
                     console.log('Reference Relation Field Structure:', JSON.stringify(relationField, null, 2));
                 }
             }

             const mMessagesId = await getId('m_messages');
             const usersId = (await getId('t_user')) || (await getId('users'));

             console.log(`Dependencies: m_messages=${mMessagesId}, users=${usersId}`);

             if (!mMessagesId || !usersId) {
                 throw new Error(`Missing dependency: m_messages=${mMessagesId}, users=${usersId}`);
             }

             const body = JSON.stringify({
                    name: 'm_reactions',
                    type: 'base',
                    fields: [
                        {
                            "id": randomId(),
                            "name": "message_id",
                            "type": "relation",
                            "system": false,
                            "required": true,
                            "collectionId": mMessagesId,
                            "cascadeDelete": true,
                            "maxSelect": 1,
                            "minSelect": null,
                            "displayFields": null
                        },
                        {
                            "id": randomId(),
                            "name": "user_id",
                            "type": "relation",
                            "system": false,
                            "required": true,
                            "collectionId": usersId,
                            "cascadeDelete": true,
                            "maxSelect": 1,
                            "minSelect": null,
                            "displayFields": null
                        },
                        {
                            "id": randomId(),
                            "name": "reaction",
                            "type": "text",
                            "system": false,
                            "required": true,
                            "max": 10
                        }
                    ],
                    indexes: [
                        'CREATE UNIQUE INDEX `idx_reactions_unique` ON `m_reactions` (`message_id`, `user_id`, `reaction`)',
                        'CREATE INDEX `idx_reactions_message` ON `m_reactions` (`message_id`)'
                    ],
                    listRule: " @request.auth.id != ''",
                    viewRule: " @request.auth.id != ''",
                    createRule: " @request.auth.id != '' && @request.auth.id = user_id",
                    updateRule: " @request.auth.id != '' && @request.auth.id = user_id",
                    deleteRule: " @request.auth.id != '' && @request.auth.id = user_id"
                });
             console.log('Sending body:', body);

             const res = await fetch(`${pbUrl}/api/collections`, {
                method: 'POST',
                headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                body: body
             });
             if (!res.ok) {
                 console.error('Failed to create m_reactions:', await res.text());
             } else {
                 console.log('✓ Created m_reactions');
             }
        }
    } catch (e) {
        console.error('Error creating m_reactions:', e);
    }

    // 3. Create m_read_receipts
    try {
        const check = await fetch(`${pbUrl}/api/collections/m_read_receipts`, { headers: { 'Authorization': token } });
        if (check.status === 404) {
            console.log('Creating m_read_receipts...');
            const mMessagesId = (await (await fetch(`${pbUrl}/api/collections/m_messages`, { headers: { 'Authorization': token } })).json()).id;
            let usersId;
             try {
                usersId = (await (await fetch(`${pbUrl}/api/collections/users`, { headers: { 'Authorization': token } })).json()).id;
             } catch {
                usersId = (await (await fetch(`${pbUrl}/api/collections/t_user`, { headers: { 'Authorization': token } })).json()).id;
             }

            await fetch(`${pbUrl}/api/collections`, {
                method: 'POST',
                headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: 'm_read_receipts',
                    type: 'base',
                    fields: [
                        {
                            "name": "messageId",
                            "type": "relation",
                            "required": true,
                            "options": {
                                "collectionId": mMessagesId,
                                "cascadeDelete": true,
                                "maxSelect": 1
                            }
                        },
                        {
                            "name": "userId",
                            "type": "relation",
                            "required": true,
                            "options": {
                                "collectionId": usersId,
                                "cascadeDelete": true,
                                "maxSelect": 1
                            }
                        },
                        {
                            "name": "readAt",
                            "type": "date",
                            "required": true
                        }
                    ],
                    indexes: [
                        'CREATE INDEX `idx_receipts_msg` ON `m_read_receipts` (`messageId`)',
                        'CREATE UNIQUE INDEX `idx_receipts_unique` ON `m_read_receipts` (`messageId`, `userId`)'
                    ],
                    listRule: " @request.auth.id != ''",
                    viewRule: " @request.auth.id != ''",
                    createRule: " @request.auth.id != '' && @request.auth.id = userId",
                    updateRule: "id = 'never_allow_updates'",
                    deleteRule: "id = 'never_allow_deletes'"
                })
            });
            console.log('✓ Created m_read_receipts');
        }
    } catch (e) {
         console.error('Error creating m_read_receipts:', e);
    }

    // 4. Create m_typing_status
    try {
         const check = await fetch(`${pbUrl}/api/collections/m_typing_status`, { headers: { 'Authorization': token } });
         if (check.status === 404) {
            console.log('Creating m_typing_status...');
            const mConversationsId = (await (await fetch(`${pbUrl}/api/collections/m_conversations`, { headers: { 'Authorization': token } })).json()).id;
            let usersId;
             try {
                usersId = (await (await fetch(`${pbUrl}/api/collections/users`, { headers: { 'Authorization': token } })).json()).id;
             } catch {
                usersId = (await (await fetch(`${pbUrl}/api/collections/t_user`, { headers: { 'Authorization': token } })).json()).id;
             }

            await fetch(`${pbUrl}/api/collections`, {
                method: 'POST',
                headers: { 'Authorization': token, 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: 'm_typing_status',
                    type: 'base',
                    fields: [
                        {
                            "name": "conversationId",
                            "type": "relation",
                            "required": true,
                            "options": {
                                "collectionId": mConversationsId,
                                "cascadeDelete": true,
                                "maxSelect": 1
                            }
                        },
                        {
                            "name": "userId",
                            "type": "relation",
                            "required": true,
                            "options": {
                                "collectionId": usersId,
                                "cascadeDelete": true,
                                "maxSelect": 1
                            }
                        },
                        {
                            "name": "isTyping",
                            "type": "bool"
                        }
                    ],
                    indexes: [
                        'CREATE INDEX `idx_typing_conv` ON `m_typing_status` (`conversationId`)'
                    ],
                    listRule: " @request.auth.id != ''",
                    viewRule: " @request.auth.id != ''",
                    createRule: " @request.auth.id != '' && @request.auth.id = userId",
                    updateRule: " @request.auth.id != '' && @request.auth.id = userId",
                    deleteRule: " @request.auth.id != '' && @request.auth.id = userId"
                })
            });
            console.log('✓ Created m_typing_status');
         }
    } catch (e) {
        console.error('Error creating m_typing_status:', e);
    }
}

export async function down(pbUrl: string, token: string) {
    console.log('Rolling back messaging features...');
    
    // Delete m_reactions
    try {
        const res = await fetch(`${pbUrl}/api/collections/m_reactions`, { headers: { 'Authorization': token } });
        if (res.ok) {
            const col = await res.json();
            await fetch(`${pbUrl}/api/collections/${col.id}`, {
                method: 'DELETE',
                headers: { 'Authorization': token }
            });
            console.log('✓ Deleted m_reactions');
        }
    } catch (e) {
        console.error('Error deleting m_reactions:', e);
    }
}