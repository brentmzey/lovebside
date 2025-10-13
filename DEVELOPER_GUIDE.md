# Quick Start Guide - Enterprise Features

## Using the Result Type

The `Result<T>` type provides type-safe error handling throughout the application.

```kotlin
// In your use case or repository
suspend fun getUser(id: String): Result<User> = retryable {
    val response = api.getUser(id)
    response
}

// In your ViewModel
fun loadUser(id: String) {
    viewModelScope.launch {
        val result = getUserUseCase(id)
        result
            .onSuccess { user -> 
                _state.value = State.Success(user)
            }
            .onError { error ->
                _state.value = State.Error(error.getUserMessage())
                logError("Failed to load user", error)
            }
            .onLoading {
                _state.value = State.Loading
            }
    }
}
```

## Error Handling

Create specific, actionable errors:

```kotlin
// Validation error
throw AppException.Validation.InvalidInput("email", "must be a valid email address")

// Network error
throw AppException.Network.Timeout()

// Business logic error
throw AppException.Business.ResourceNotFound("User", userId)

// Auth error
throw AppException.Auth.SessionExpired()
```

## Logging

Use extension functions for easy logging:

```kotlin
class MyRepository {
    suspend fun fetchData() {
        logDebug("Fetching data...")
        try {
            val data = api.getData()
            logInfo("Data fetched successfully: ${data.size} items")
            return data
        } catch (e: Exception) {
            logError("Failed to fetch data", e)
            throw e
        }
    }
}
```

## Validation

Validate user input before processing:

```kotlin
fun validateLoginForm(email: String, password: String): ValidationResult {
    val emailValidation = Validators.validateEmail(email)
    val passwordValidation = Validators.validatePassword(password)
    return Validators.combine(emailValidation, passwordValidation)
}

// Usage
val validation = validateLoginForm(email, password)
if (validation.isValid) {
    performLogin()
} else {
    showError(validation.getErrorOrNull()?.getUserMessage())
}
```

## Caching

Use the in-memory cache for performance:

```kotlin
class UserRepository(
    private val api: ApiClient,
    private val cache: InMemoryCache<String, User> = InMemoryCache()
) {
    suspend fun getUser(userId: String): Result<User> = retryable {
        cache.getOrPut(CacheKeys.userProfile(userId)) {
            api.getUser(userId)
        }
    }
}
```

## Configuration

Access app configuration:

```kotlin
val config = appConfig()

if (config.environment == Environment.DEVELOPMENT) {
    enableDebugFeatures()
}

if (config.features.enableBiometricAuth) {
    setupBiometricAuth()
}

// Use configured values
httpClient.timeout = config.apiTimeout
retryPolicy.maxAttempts = config.maxRetryAttempts
```

## UI Components

Use pre-built components for consistent UX:

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.state.collectAsState()
    
    when (val currentState = state) {
        is State.Loading -> LoadingView(message = "Loading data...")
        is State.Success -> SuccessContent(currentState.data)
        is State.Error -> ErrorScreen(
            exception = currentState.error,
            onRetry = { viewModel.retry() }
        )
        is State.Empty -> EmptyView(
            message = "No data available",
            icon = "📭",
            actionLabel = "Refresh",
            onAction = { viewModel.refresh() }
        )
    }
}

@Composable
fun LoginForm() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    Column {
        ValidatedTextField(
            value = email,
            onValueChange = { 
                email = it
                emailError = Validators.validateEmail(it).getErrorOrNull()?.getUserMessage()
            },
            label = "Email",
            errorMessage = emailError,
            keyboardType = KeyboardType.Email
        )
        
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password"
        )
        
        LoadingButton(
            onClick = { performLogin() },
            text = "Login",
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

## Network Resilience

All API calls automatically benefit from retry logic:

```kotlin
// This will automatically retry on network failures
suspend fun loadMatches(): Result<List<Match>> = retryable {
    apiClient.getMatches()
}

// Custom retry configuration
suspend fun uploadLargeFile(file: File): Result<Unit> = 
    NetworkResilience.withRetry(
        maxAttempts = 5,
        initialDelayMs = 2000L,
        factor = 2.5
    ) {
        apiClient.uploadFile(file)
    }
```

## Platform-Specific Code

When you need platform-specific implementations:

```kotlin
// In commonMain
expect fun getPlatformName(): String
expect fun saveToDisk(data: ByteArray): Boolean

// In androidMain
actual fun getPlatformName() = "Android"
actual fun saveToDisk(data: ByteArray): Boolean {
    // Android-specific file saving
}

// In iosMain  
actual fun getPlatformName() = "iOS"
actual fun saveToDisk(data: ByteArray): Boolean {
    // iOS-specific file saving
}
```

## Testing

Structure for testing:

```kotlin
class UserRepositoryTest {
    private lateinit var mockApi: ApiClient
    private lateinit var cache: InMemoryCache<String, User>
    private lateinit var repository: UserRepository
    
    @Before
    fun setup() {
        mockApi = mockk()
        cache = InMemoryCache()
        repository = UserRepository(mockApi, cache)
    }
    
    @Test
    fun `getUser returns cached value when available`() = runTest {
        // Given
        val userId = "123"
        val cachedUser = User(userId, "John")
        cache.put(CacheKeys.userProfile(userId), cachedUser)
        
        // When
        val result = repository.getUser(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(cachedUser, result.getOrNull())
        verify(exactly = 0) { mockApi.getUser(any()) }
    }
    
    @Test
    fun `getUser fetches from API when cache miss`() = runTest {
        // Given
        val userId = "123"
        val apiUser = User(userId, "John")
        coEvery { mockApi.getUser(userId) } returns apiUser
        
        // When
        val result = repository.getUser(userId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(apiUser, result.getOrNull())
        coVerify(exactly = 1) { mockApi.getUser(userId) }
    }
}
```

## Common Patterns

### ViewModel State Management
```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: AppException) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
}

class MyViewModel(
    private val useCase: GetDataUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<Data>>(UiState.Loading)
    val state: StateFlow<UiState<Data>> = _state.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            useCase()
                .onSuccess { data ->
                    _state.value = if (data.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(data)
                    }
                }
                .onError { error ->
                    _state.value = UiState.Error(error)
                    logError("Failed to load data", error)
                }
        }
    }
}
```

### Repository Pattern
```kotlin
interface DataRepository {
    suspend fun getData(id: String): Result<Data>
    suspend fun saveData(data: Data): Result<Unit>
    suspend fun deleteData(id: String): Result<Unit>
}

class DataRepositoryImpl(
    private val api: ApiClient,
    private val cache: InMemoryCache<String, Data>
) : DataRepository {
    override suspend fun getData(id: String): Result<Data> = retryable {
        cache.getOrPut(CacheKeys.data(id)) {
            api.getData(id)
        }
    }
    
    override suspend fun saveData(data: Data): Result<Unit> = retryable {
        api.saveData(data).also {
            cache.put(CacheKeys.data(data.id), data)
        }
    }
    
    override suspend fun deleteData(id: String): Result<Unit> = retryable {
        api.deleteData(id).also {
            cache.remove(CacheKeys.data(id))
        }
    }
}
```

### Use Case Pattern
```kotlin
class GetUserProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String): Result<UserProfile> {
        logDebug("Getting user profile for: $userId")
        
        // Validate input
        if (userId.isBlank()) {
            return Result.Error(
                AppException.Validation.RequiredField("User ID")
            )
        }
        
        // Fetch from repository
        return repository.getUserProfile(userId)
            .map { profile ->
                // Transform if needed
                profile.copy(
                    displayName = profile.displayName.trim()
                )
            }
    }
}
```

## Best Practices

1. **Always use Result type** for operations that can fail
2. **Log important operations** at appropriate levels
3. **Validate input early** before processing
4. **Cache when appropriate** to reduce network calls
5. **Use sealed classes** for exhaustive state handling
6. **Leverage coroutines** for async operations
7. **Keep ViewModels thin** - business logic in use cases
8. **Write tests** for critical paths
9. **Document public APIs** with KDoc
10. **Follow platform conventions** in platform-specific code

## Troubleshooting

### Common Issues

**Issue**: "Unresolved reference to Koin"
- **Solution**: Check that you're in a platform that supports Koin (not WasmJS)

**Issue**: "Compilation error in Result.kt"
- **Solution**: Ensure you're using the correct variance (in/out) for generic types

**Issue**: "Network timeout"
- **Solution**: Check `appConfig().apiTimeout` and adjust if needed

**Issue**: "Cache not working"
- **Solution**: Ensure TTL hasn't expired, call `cache.get()` to check

**Issue**: "Validation not showing errors"
- **Solution**: Check that you're calling `.getErrorOrNull()` and displaying it

## Performance Tips

1. **Cache aggressively** for read-heavy data
2. **Use pagination** for large lists (page size in Constants)
3. **Lazy load** heavy resources
4. **Cancel coroutines** when screens close
5. **Use remember** in Compose for expensive calculations
6. **Profile regularly** using platform tools
7. **Monitor memory** with cache cleanup
8. **Optimize images** before display
9. **Debounce** search and input operations
10. **Use LazyColumn** for long lists

## Security Reminders

1. **Never log sensitive data** (passwords, tokens, PII)
2. **Use HTTPS only** for API calls
3. **Validate all input** before processing
4. **Store tokens securely** using platform keystore
5. **Implement certificate pinning** for production
6. **Obfuscate code** with ProGuard/R8
7. **Check for root/jailbreak** on sensitive operations
8. **Use biometric auth** where available
9. **Clear sensitive data** on logout
10. **Follow OWASP guidelines** for mobile apps
