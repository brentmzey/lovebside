/// <reference path="../pb_data/types.d.ts" />

/**
 * Media Storage Optimization Migration
 * 
 * Purpose:
 * - Add CDN URI fields for external media storage (S3/CloudFront)
 * - Optimize file fields for better performance
 * - Add media metadata tracking
 * - Prepare for migration from PocketBase storage to S3
 * 
 * Migration Strategy:
 * 1. Phase 1: Dual storage (PocketBase + S3 URI references) - THIS MIGRATION
 * 2. Phase 2: Gradual migration of existing files to S3
 * 3. Phase 3: Switch to S3-only with PocketBase storing only URIs
 */

migrate((db) => {
  const dao = new Dao(db);

  // ==============================================
  // 1. MESSAGES COLLECTION - MEDIA OPTIMIZATION
  // ==============================================
  try {
    const messagesCollection = dao.findCollectionByNameOrId("m_messages"); // m_messages
    
    // Add CDN URI field for attachments (for S3/CloudFront migration)
    messagesCollection.schema.addField(new SchemaField({
      system: false,
      id: "json_cdn_uris",
      name: "cdn_attachment_uris",
      type: "json",
      required: false,
      presentable: false,
      options: {
        maxSize: 2000000 // 2MB for JSON metadata
      }
    }));

    // Add media metadata tracking
    messagesCollection.schema.addField(new SchemaField({
      system: false,
      id: "json_media_meta",
      name: "media_metadata",
      type: "json",
      required: false,
      presentable: false,
      options: {
        maxSize: 1000000
      }
    }));

    // Update attachments field for better performance
    const attachmentsField = messagesCollection.schema.getFieldById("file1204091606");
    if (attachmentsField) {
      attachmentsField.options = {
        maxSelect: 20,
        maxSize: 52428800, // 50MB per file
        mimeTypes: [
          // Images
          "image/jpeg",
          "image/png", 
          "image/webp",
          "image/gif",
          "image/svg+xml",
          // Videos
          "video/mp4",
          "video/webm",
          "video/quicktime",
          // Documents
          "application/pdf",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          // Audio
          "audio/mpeg",
          "audio/wav",
          "audio/ogg"
        ],
        thumbs: [
          "100x100",   // Thumbnail
          "400x400",   // Small preview
          "800x800",   // Medium preview
          "1200x1200"  // Large preview
        ],
        protected: false
      };
    }

    dao.saveCollection(messagesCollection);
    console.log("✅ Updated m_messages for media optimization");

  } catch (e) {
    console.error("❌ Error updating m_messages:", e);
  }

  // ==============================================
  // 2. PROFILES COLLECTION - MEDIA OPTIMIZATION
  // ==============================================
  try {
    const profilesCollection = dao.findCollectionByNameOrId("s_profiles"); // s_profiles
    
    // Add CDN URI for profile picture
    profilesCollection.schema.addField(new SchemaField({
      system: false,
      id: "text_cdn_profile_pic",
      name: "cdn_profile_picture_uri",
      type: "text",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: 500,
        pattern: ""
      }
    }));

    // Add CDN URIs for photos gallery
    profilesCollection.schema.addField(new SchemaField({
      system: false,
      id: "json_cdn_photos",
      name: "cdn_photos_uris",
      type: "json",
      required: false,
      presentable: false,
      options: {
        maxSize: 500000
      }
    }));

    // Update profile picture field
    const profilePicField = profilesCollection.schema.getFieldById("file1216345841");
    if (profilePicField) {
      profilePicField.options = {
        maxSelect: 1,
        maxSize: 10485760, // 10MB
        mimeTypes: [
          "image/jpeg",
          "image/png",
          "image/webp",
          "image/gif"
        ],
        thumbs: [
          "50x50",     // Tiny avatar
          "100x100",   // Small avatar
          "200x200",   // Medium avatar
          "400x400",   // Large avatar
          "800x800"    // Full resolution
        ],
        protected: false
      };
    }

    // Update photos gallery field
    const photosField = profilesCollection.schema.getFieldById("file142008537");
    if (photosField) {
      photosField.options = {
        maxSelect: 15,
        maxSize: 10485760, // 10MB per photo
        mimeTypes: [
          "image/jpeg",
          "image/png",
          "image/webp",
          "image/gif"
        ],
        thumbs: [
          "200x200",   // Thumbnail
          "600x600",   // Grid view
          "1200x1200"  // Full view
        ],
        protected: false
      };
    }

    dao.saveCollection(profilesCollection);
    console.log("✅ Updated s_profiles for media optimization");

  } catch (e) {
    console.error("❌ Error updating s_profiles:", e);
  }

  // ==============================================
  // 3. CONVERSATIONS COLLECTION - AVATAR OPTIMIZATION
  // ==============================================
  try {
    const conversationsCollection = dao.findCollectionByNameOrId("m_conversations"); // m_conversations
    
    // Add CDN URI for conversation avatar
    conversationsCollection.schema.addField(new SchemaField({
      system: false,
      id: "text_cdn_avatar",
      name: "cdn_avatar_uri",
      type: "text",
      required: false,
      presentable: false,
      options: {
        min: 0,
        max: 500,
        pattern: ""
      }
    }));

    // Update avatar field
    const avatarField = conversationsCollection.schema.getFieldById("file2402662569");
    if (avatarField) {
      avatarField.options = {
        maxSelect: 1,
        maxSize: 5242880, // 5MB
        mimeTypes: [
          "image/jpeg",
          "image/png",
          "image/webp",
          "image/gif"
        ],
        thumbs: [
          "50x50",
          "100x100",
          "200x200"
        ],
        protected: false
      };
    }

    dao.saveCollection(conversationsCollection);
    console.log("✅ Updated m_conversations for media optimization");

  } catch (e) {
    console.error("❌ Error updating m_conversations:", e);
  }

  // ==============================================
  // 4. CREATE MEDIA MIGRATION TRACKING COLLECTION
  // ==============================================
  try {
    const mediaTracking = new Collection({
      name: "system_media_migration",
      type: "base",
      system: false,
      schema: [
        {
          system: false,
          id: "text_source_col",
          name: "source_collection",
          type: "text",
          required: true,
          presentable: false,
          options: {
            min: 0,
            max: 100,
            pattern: ""
          }
        },
        {
          system: false,
          id: "text_source_rec",
          name: "source_record_id",
          type: "text",
          required: true,
          presentable: false,
          options: {
            min: 15,
            max: 15,
            pattern: "^[a-z0-9]+$"
          }
        },
        {
          system: false,
          id: "text_source_field",
          name: "source_field_name",
          type: "text",
          required: true,
          presentable: false,
          options: {
            min: 0,
            max: 100,
            pattern: ""
          }
        },
        {
          system: false,
          id: "text_local_path",
          name: "local_file_path",
          type: "text",
          required: true,
          presentable: false,
          options: {
            min: 0,
            max: 500,
            pattern: ""
          }
        },
        {
          system: false,
          id: "text_cdn_uri",
          name: "cdn_uri",
          type: "text",
          required: false,
          presentable: false,
          options: {
            min: 0,
            max: 1000,
            pattern: ""
          }
        },
        {
          system: false,
          id: "select_status",
          name: "migration_status",
          type: "select",
          required: true,
          presentable: false,
          options: {
            maxSelect: 1,
            values: [
              "pending",
              "uploading",
              "completed",
              "failed",
              "verified"
            ]
          }
        },
        {
          system: false,
          id: "json_metadata",
          name: "file_metadata",
          type: "json",
          required: false,
          presentable: false,
          options: {
            maxSize: 100000
          }
        },
        {
          system: false,
          id: "date_migrated",
          name: "migrated_at",
          type: "date",
          required: false,
          presentable: false,
          options: {
            min: "",
            max: ""
          }
        },
        {
          system: false,
          id: "text_error",
          name: "error_message",
          type: "text",
          required: false,
          presentable: false,
          options: {
            min: 0,
            max: 1000,
            pattern: ""
          }
        }
      ],
      indexes: [
        "CREATE INDEX idx_media_migration_status ON system_media_migration (migration_status)",
        "CREATE INDEX idx_media_migration_source ON system_media_migration (source_collection, source_record_id)",
        "CREATE UNIQUE INDEX idx_media_migration_unique ON system_media_migration (source_collection, source_record_id, source_field_name, local_file_path)"
      ],
      listRule: "@request.auth.id != ''",
      viewRule: "@request.auth.id != ''",
      createRule: null,
      updateRule: null,
      deleteRule: null
    });

    dao.saveCollection(mediaTracking);
    console.log("✅ Created system_media_migration tracking collection");

  } catch (e) {
    console.error("❌ Error creating media migration tracking:", e);
  }

}, (db) => {
  // Rollback - remove added fields and collection
  const dao = new Dao(db);

  try {
    // Remove fields from m_messages
    const messagesCollection = dao.findCollectionByNameOrId("m_messages");
    messagesCollection.schema.removeField("json_cdn_uris");
    messagesCollection.schema.removeField("json_media_meta");
    dao.saveCollection(messagesCollection);
  } catch (e) {
    console.error("Rollback error for m_messages:", e);
  }

  try {
    // Remove fields from s_profiles
    const profilesCollection = dao.findCollectionByNameOrId("s_profiles");
    profilesCollection.schema.removeField("text_cdn_profile_pic");
    profilesCollection.schema.removeField("json_cdn_photos");
    dao.saveCollection(profilesCollection);
  } catch (e) {
    console.error("Rollback error for s_profiles:", e);
  }

  try {
    // Remove fields from m_conversations
    const conversationsCollection = dao.findCollectionByNameOrId("m_conversations");
    conversationsCollection.schema.removeField("text_cdn_avatar");
    dao.saveCollection(conversationsCollection);
  } catch (e) {
    console.error("Rollback error for m_conversations:", e);
  }

  try {
    // Delete tracking collection
    const tracking = dao.findCollectionByNameOrId("system_media_migration");
    dao.deleteCollection(tracking);
  } catch (e) {
    console.error("Rollback error for system_media_migration:", e);
  }
});
