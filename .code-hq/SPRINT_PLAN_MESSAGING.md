# Messaging Features - Sprint Action Plan

## Summary

Migration work successfully completed for core messaging schema. Now need to:
1. Verify prod/local sync
2. Build APIs for reactions/presence
3. Implement frontend UIs

## Completed ✅

- [X] m_reactions migration (1769800000) - APPLIED & TESTED
- [X] m_presence migration (1769810000) - CREATED, ready to apply
- [X] Schema documentation (MESSAGING_STATUS.md)
- [X] Roadmap updates (ROADMAP_PHASE_2.md)

## Next 3 Sprints

### Sprint 1: Schema Sync & Reactions API (Week 1)

**Goals**: Schema parity between environments, working reactions

1. **Schema Verification**
   - [ ] Apply m_presence migration locally
   - [ ] Export current local schema to full_schema.json
   - [ ] Compare with production schema
   - [ ] Document differences
   - [ ] Create migration plan for prod

2. **Reactions Backend** 
   - [ ] Create Ktor API endpoints:
     - `POST /api/v1/messages/{id}/reactions` - Add reaction
     - `DELETE /api/v1/messages/{id}/reactions/{reaction}` - Remove reaction
     - `GET /api/v1/messages/{id}/reactions` - List reactions
   - [ ] Add real-time events via PocketBase SSE
   - [ ] Write unit tests for reaction logic
   - [ ] Write integration tests

3. **Reactions Frontend**
   - [ ] Create ReactionPicker component
   - [ ] Add reaction buttons to MessageItem
   - [ ] Show reaction counts with user lists
   - [ ] Handle real-time reaction updates
   - [ ] Write Compose UI tests

**Deliverable**: Users can add/remove reactions on messages with real-time updates

### Sprint 2: Presence & Typing Indicators (Weeks 2-3)

**Goals**: Show who's online and typing

1. **Presence Backend**
   - [ ] WebSocket presence tracking
   - [ ] Heartbeat mechanism (30s)
   - [ ] Auto-away after 5min inactivity
   - [ ] Status change API endpoints
   - [ ] Activity message support

2. **Typing Indicators Backend**
   - [ ] WebSocket typing events
   - [ ] Debounce logic (stopped typing after 3s)
   - [ ] Per-conversation typing status

3. **Presence Frontend**
   - [ ] Online status dots on avatars
   - [ ] Activity status in profiles
   - [ ] Custom status messages
   - [ ] Settings for presence visibility

4. **Typing Indicators Frontend**
   - [ ] "X is typing..." indicator in chat
   - [ ] Multi-user typing support
   - [ ] Animation for typing dots
   - [ ] Send typing events on input

**Deliverable**: Users see who's online and when someone is typing

### Sprint 3: Polish & Read Receipts (Week 4)

**Goals**: Professional message experience

1. **Read Receipts**
   - [ ] Automatic read tracking on message view
   - [ ] Batch read receipt updates
   - [ ] Show read/delivered status
   - [ ] Privacy settings for read receipts

2. **Message Actions**
   - [ ] Edit message (within 15min)
   - [ ] Delete message (with cascade)
   - [ ] Forward message
   - [ ] Pin important messages

3. **Performance**
   - [ ] Message pagination (cursor-based)
   - [ ] Image lazy loading
   - [ ] Virtual scrolling for long chats
   - [ ] Optimize query indexes

4. **Testing & Benchmarks**
   - [ ] E2E test: Send message with reaction
   - [ ] E2E test: Presence updates
   - [ ] Load test: 100 concurrent users
   - [ ] Performance benchmark: Message latency

**Deliverable**: Production-ready messaging with observability

## Future Sprints (Backlog)

### Phase 2.5: Rich Features
- Mentions with autocomplete
- Polls creation/voting
- Link preview cards
- Voice messages

### Phase 3: Advanced
- Disappearing messages
- E2E encryption
- Message search
- Multi-device sync

## Migration Guidelines Reminder

**EVERY migration MUST**:
1. Check if collection/field exists (idempotency)
2. Include try-catch for rollback safety
3. Stage operations:
   - Create collection first
   - Add relations separately
   - Add indexes last
   - Update rules last
4. Log operations for debugging
5. Include rollback in down migration

**Example Pattern** (from m_presence):
```javascript
migrate((app) => {
  // Idempotency check
  try {
    const existing = app.findCollectionByNameOrId("collection_name");
    if (existing) return;
  } catch (e) {}
  
  // 1. Create basic collection
  const collection = new Collection({...});
  app.save(collection);
  
  // 2. Add relations
  const created = app.findCollectionByNameOrId("collection_name");
  created.fields.addAt(index, new Field({...}));
  
  // 3. Add indexes
  created.indexes = [...];
  
  // 4. Add rules
  created.listRule = "...";
  
  return app.save(created);
}, (app) => {
  // Rollback with try-catch
  try {
    const collection = app.findCollectionByNameOrId("collection_name");
    return app.delete(collection);
  } catch (e) {}
})
```

## Success Metrics

- Message delivery < 100ms (p95)
- Typing indicator < 50ms (p95)
- Reaction animation 60fps
- Zero data loss on crashes
- 99.9% uptime for messaging

## Documentation TODOs

- [ ] API documentation (OpenAPI spec)
- [ ] Real-time events documentation
- [ ] Frontend integration guide
- [ ] Performance benchmarking results
- [ ] Migration runbook for prod deployment

---

**Last Updated**: 2026-01-30
**Status**: Ready for Sprint 1 kickoff
**Next Review**: End of Week 1
