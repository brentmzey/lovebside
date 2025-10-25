// Migration: Add profile pictures and photo gallery to profiles
// Date: 2025-01-19
// Purpose: Enable users to upload profile pictures and additional photos

migrate((db) => {
  const dao = new Dao(db);
  
  // Get existing profiles collection
  const profilesCollection = dao.findCollectionByNameOrId("s_profiles");
  
  // Add profile picture field (single file)
  profilesCollection.schema.addField(new SchemaField({
    "name": "profilePicture",
    "type": "file",
    "required": false,
    "options": {
      "maxSelect": 1,
      "maxSize": 5242880, // 5MB
      "mimeTypes": [
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif"
      ],
      "thumbs": [
        "100x100",   // Thumbnail
        "512x512"    // Profile view
      ],
      "protected": false
    }
  }));
  
  // Add photos gallery field (multiple files)
  profilesCollection.schema.addField(new SchemaField({
    "name": "photos",
    "type": "file",
    "required": false,
    "options": {
      "maxSelect": 10, // Max 10 photos
      "maxSize": 10485760, // 10MB per photo
      "mimeTypes": [
        "image/jpeg",
        "image/png",
        "image/webp"
      ],
      "thumbs": [
        "200x200",   // Grid thumbnail
        "800x800"    // Full view
      ],
      "protected": false
    }
  }));
  
  // Add aboutMe field for richer profile description
  profilesCollection.schema.addField(new SchemaField({
    "name": "aboutMe",
    "type": "editor",
    "required": false,
    "options": {
      "maxSize": 2000 // 2000 characters
    }
  }));
  
  // Add height field (optional)
  profilesCollection.schema.addField(new SchemaField({
    "name": "height",
    "type": "number",
    "required": false,
    "options": {
      "min": 120, // cm
      "max": 250
    }
  }));
  
  // Add occupation field
  profilesCollection.schema.addField(new SchemaField({
    "name": "occupation",
    "type": "text",
    "required": false,
    "options": {
      "min": null,
      "max": 100
    }
  }));
  
  // Add education field
  profilesCollection.schema.addField(new SchemaField({
    "name": "education",
    "type": "text",
    "required": false,
    "options": {
      "min": null,
      "max": 200
    }
  }));
  
  // Add interests field (multi-select tags)
  profilesCollection.schema.addField(new SchemaField({
    "name": "interests",
    "type": "select",
    "required": false,
    "options": {
      "maxSelect": 10,
      "values": [
        "Art",
        "Music",
        "Sports",
        "Travel",
        "Cooking",
        "Reading",
        "Photography",
        "Gaming",
        "Fitness",
        "Technology",
        "Nature",
        "Movies",
        "Theater",
        "Dancing",
        "Writing",
        "Volunteering",
        "Pets",
        "Fashion",
        "Food",
        "Science"
      ]
    }
  }));
  
  dao.saveCollection(profilesCollection);
  
}, (db) => {
  // Rollback: Remove added fields
  const dao = new Dao(db);
  const profilesCollection = dao.findCollectionByNameOrId("s_profiles");
  
  // Remove fields in reverse order
  profilesCollection.schema.removeField("interests");
  profilesCollection.schema.removeField("education");
  profilesCollection.schema.removeField("occupation");
  profilesCollection.schema.removeField("height");
  profilesCollection.schema.removeField("aboutMe");
  profilesCollection.schema.removeField("photos");
  profilesCollection.schema.removeField("profilePicture");
  
  dao.saveCollection(profilesCollection);
});
