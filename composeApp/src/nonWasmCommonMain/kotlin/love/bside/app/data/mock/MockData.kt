package love.bside.app.data.mock

import love.bside.app.data.models.Profile
import love.bside.app.data.models.SeekingStatus
import love.bside.app.data.models.messaging.Conversation

/**
 * Mock data for UI testing and development
 */
object MockData {
    
    // Mock current user profile
    val currentUser = Profile(
        id = "user_001",
        collectionId = "profiles",
        collectionName = "profiles",
        created = "2025-10-01T12:00:00Z",
        updated = "2025-10-19T19:00:00Z",
        userId = "user_001",
        firstName = "Alex",
        lastName = "Chen",
        birthDate = "1995-06-15",
        bio = "Coffee enthusiast, adventure seeker, and lifelong learner. Love deep conversations about philosophy, technology, and what makes us human.",
        location = "San Francisco, CA",
        seeking = SeekingStatus.BOTH,
        profilePicture = null,
        photos = null,
        aboutMe = "I'm passionate about building meaningful connections and exploring new ideas. Whether it's hiking in nature, diving into a great book, or debating life's big questions over coffee, I'm always up for authentic experiences.",
        height = 175,
        occupation = "Product Designer",
        education = "B.S. Computer Science",
        interests = listOf("Hiking", "Coffee", "Philosophy", "Design", "Travel", "Photography")
    )
    
    // Mock user profiles for discovery
    val discoverProfiles = listOf(
        Profile(
            id = "user_002",
            collectionId = "profiles",
            collectionName = "profiles",
            created = "2025-09-15T10:00:00Z",
            updated = "2025-10-18T15:30:00Z",
            userId = "user_002",
            firstName = "Jordan",
            lastName = "Rivera",
            birthDate = "1993-03-22",
            bio = "Artist and dreamer. Finding beauty in everyday moments.",
            location = "Oakland, CA",
            seeking = SeekingStatus.FRIENDSHIP,
            profilePicture = null,
            photos = null,
            aboutMe = "Visual storyteller with a passion for urban art and community building.",
            height = 168,
            occupation = "Graphic Designer",
            education = "B.A. Fine Arts",
            interests = listOf("Art", "Music", "Street Photography", "Yoga")
        ),
        Profile(
            id = "user_003",
            collectionId = "profiles",
            collectionName = "profiles",
            created = "2025-08-20T08:00:00Z",
            updated = "2025-10-19T12:00:00Z",
            userId = "user_003",
            firstName = "Sam",
            lastName = "Taylor",
            birthDate = "1996-11-08",
            bio = "Tech geek meets outdoor enthusiast. Building the future while staying grounded.",
            location = "Berkeley, CA",
            seeking = SeekingStatus.RELATIONSHIP,
            profilePicture = null,
            photos = null,
            aboutMe = "Software engineer by day, rock climber by weekend. Love solving problems and pushing limits.",
            height = 180,
            occupation = "Software Engineer",
            education = "M.S. Computer Science",
            interests = listOf("Rock Climbing", "Coding", "Board Games", "Cooking")
        ),
        Profile(
            id = "user_004",
            collectionId = "profiles",
            collectionName = "profiles",
            created = "2025-09-01T14:00:00Z",
            updated = "2025-10-17T09:00:00Z",
            userId = "user_004",
            firstName = "Casey",
            lastName = "Morgan",
            birthDate = "1994-07-30",
            bio = "Bookworm and coffee addict. Always looking for the next great conversation.",
            location = "San Francisco, CA",
            seeking = SeekingStatus.BOTH,
            profilePicture = null,
            photos = null,
            aboutMe = "I believe in the power of stories to connect us. Whether through books, films, or real conversations.",
            height = 165,
            occupation = "Writer",
            education = "M.F.A. Creative Writing",
            interests = listOf("Reading", "Writing", "Coffee Shops", "Poetry", "Film")
        ),
        Profile(
            id = "user_005",
            collectionId = "profiles",
            collectionName = "profiles",
            created = "2025-10-05T11:00:00Z",
            updated = "2025-10-19T18:00:00Z",
            userId = "user_005",
            firstName = "Riley",
            lastName = "Park",
            birthDate = "1997-01-12",
            bio = "Foodie and explorer. Life is too short for boring meals or shallow conversations.",
            location = "San Jose, CA",
            seeking = SeekingStatus.FRIENDSHIP,
            profilePicture = null,
            photos = null,
            aboutMe = "Culinary adventurer always searching for the perfect bite and meaningful connections.",
            height = 172,
            occupation = "Chef",
            education = "Culinary Institute",
            interests = listOf("Cooking", "Food Photography", "Travel", "Wine Tasting")
        ),
        Profile(
            id = "user_006",
            collectionId = "profiles",
            collectionName = "profiles",
            created = "2025-09-10T16:00:00Z",
            updated = "2025-10-16T20:00:00Z",
            userId = "user_006",
            firstName = "Morgan",
            lastName = "Lee",
            birthDate = "1995-04-25",
            bio = "Scientist with a soul. Curious about everything from quantum physics to human nature.",
            location = "Palo Alto, CA",
            seeking = SeekingStatus.RELATIONSHIP,
            profilePicture = null,
            photos = null,
            aboutMe = "Researcher exploring the boundaries of knowledge and connection.",
            height = 178,
            occupation = "Research Scientist",
            education = "Ph.D. Physics",
            interests = listOf("Science", "Astronomy", "Meditation", "Jazz Music")
        )
    )
    
    // Mock conversations
    val conversations = listOf(
        Conversation(
            id = "conv_001",
            participant1Id = "user_001",
            participant2Id = "user_002",
            lastMessageText = "That sounds amazing! I'd love to check out that gallery opening this weekend.",
            lastMessageAt = "2025-10-19T18:30:00Z",
            participant1UnreadCount = 2,
            participant2UnreadCount = 0,
            participant1LastReadAt = "2025-10-19T17:00:00Z",
            participant2LastReadAt = "2025-10-19T18:30:00Z",
            created = "2025-10-15T10:00:00Z",
            updated = "2025-10-19T18:30:00Z"
        ),
        Conversation(
            id = "conv_002",
            participant1Id = "user_001",
            participant2Id = "user_003",
            lastMessageText = "Just finished that climb! The view from the top was incredible 🏔️",
            lastMessageAt = "2025-10-19T16:45:00Z",
            participant1UnreadCount = 0,
            participant2UnreadCount = 1,
            participant1LastReadAt = "2025-10-19T16:45:00Z",
            participant2LastReadAt = "2025-10-19T14:00:00Z",
            created = "2025-10-12T14:30:00Z",
            updated = "2025-10-19T16:45:00Z"
        ),
        Conversation(
            id = "conv_003",
            participant1Id = "user_001",
            participant2Id = "user_004",
            lastMessageText = "I just started reading that book you recommended - it's incredible so far!",
            lastMessageAt = "2025-10-19T12:15:00Z",
            participant1UnreadCount = 0,
            participant2UnreadCount = 0,
            participant1LastReadAt = "2025-10-19T12:20:00Z",
            participant2LastReadAt = "2025-10-19T12:15:00Z",
            created = "2025-10-08T09:00:00Z",
            updated = "2025-10-19T12:15:00Z"
        ),
        Conversation(
            id = "conv_004",
            participant1Id = "user_001",
            participant2Id = "user_005",
            lastMessageText = "We should totally grab coffee next week and talk about that food festival!",
            lastMessageAt = "2025-10-18T20:00:00Z",
            participant1UnreadCount = 1,
            participant2UnreadCount = 0,
            participant1LastReadAt = "2025-10-18T19:00:00Z",
            participant2LastReadAt = "2025-10-18T20:00:00Z",
            created = "2025-10-10T15:00:00Z",
            updated = "2025-10-18T20:00:00Z"
        ),
        Conversation(
            id = "conv_005",
            participant1Id = "user_001",
            participant2Id = "user_006",
            lastMessageText = "That documentary on black holes blew my mind! Want to discuss over dinner?",
            lastMessageAt = "2025-10-17T19:30:00Z",
            participant1UnreadCount = 0,
            participant2UnreadCount = 0,
            participant1LastReadAt = "2025-10-17T20:00:00Z",
            participant2LastReadAt = "2025-10-17T19:30:00Z",
            created = "2025-10-05T11:00:00Z",
            updated = "2025-10-17T19:30:00Z"
        )
    )
    
    // Get profile by ID
    fun getProfileById(userId: String): Profile? {
        return when (userId) {
            currentUser.userId -> currentUser
            else -> discoverProfiles.find { it.userId == userId }
        }
    }
    
    // Get user name for conversation
    fun getUserNameForConversation(userId: String): String {
        return getProfileById(userId)?.getDisplayName() ?: "User"
    }
}
