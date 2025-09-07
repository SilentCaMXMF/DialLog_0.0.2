# Contributing to DialLog

First off, thank you for considering contributing to DialLog! It's people like you that make DialLog such a great tool for improving communication.

## Code of Conduct

This project and everyone participating in it is governed by our commitment to creating a welcoming and inclusive environment. By participating, you are expected to uphold this standard.

## How Can I Contribute?

### 🐛 Reporting Bugs

Before creating bug reports, please check the existing issues as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

**Before Submitting A Bug Report**
- Check if you can reproduce the problem in the latest version
- Check if the problem has already been reported by searching on GitHub under Issues

**How Do I Submit A (Good) Bug Report?**
Bugs are tracked as GitHub issues. Create an issue and provide the following information:

- **Use a clear and descriptive title**
- **Describe the exact steps which reproduce the problem**
- **Provide specific examples to demonstrate the steps**
- **Describe the behavior you observed after following the steps**
- **Explain which behavior you expected to see instead and why**
- **Include screenshots and animated GIFs** which show you following the described steps
- **Include device information** (Android version, device model, app version)

### 💡 Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. Create an issue and provide the following information:

- **Use a clear and descriptive title**
- **Provide a step-by-step description of the suggested enhancement**
- **Provide specific examples to demonstrate the steps**
- **Describe the current behavior** and **explain which behavior you expected to see instead**
- **Explain why this enhancement would be useful** to most DialLog users

### 🔧 Code Contributions

#### Local Development Setup

1. **Prerequisites**
   - Android Studio Arctic Fox (2020.3.1) or newer
   - JDK 11 or newer
   - Android SDK 21 or higher
   - Git

2. **Fork and Clone**
   ```bash
   # Fork the repo on GitHub, then clone your fork
   git clone https://github.com/YOUR-USERNAME/DialLog.git
   cd DialLog
   ```

3. **Set up the development environment**
   ```bash
   # Open the project in Android Studio
   # Let Gradle sync and install dependencies
   # Connect an Android device or start an emulator
   ```

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   # Or run directly from Android Studio
   ```

#### Pull Request Process

1. **Create a branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

2. **Make your changes**
   - Follow the existing code style
   - Add comments for complex logic
   - Update documentation if needed
   - Add tests if applicable

3. **Test your changes**
   - Test on multiple Android versions if possible
   - Test with different device configurations
   - Ensure existing functionality still works

4. **Commit your changes**
   ```bash
   git add .
   git commit -m "Add feature: your feature description"
   # Use conventional commit format if possible
   ```

5. **Push and create PR**
   ```bash
   git push origin feature/your-feature-name
   # Then create a Pull Request on GitHub
   ```

## 🎨 Style Guidelines

### Code Style
- **Language**: Kotlin preferred, Java acceptable
- **Formatting**: Use Android Studio default formatting (Ctrl+Alt+L)
- **Naming**: Use descriptive names for variables, functions, and classes
- **Comments**: Add KDoc comments for public functions and classes

### Git Commit Messages
- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line

Example:
```
Add real-time call duration display

- Show elapsed time during active calls
- Update UI every second
- Format time as MM:SS

Fixes #123
```

### Kotlin Style Guide
```kotlin
// Class names: PascalCase
class CallTracker {
    
    // Function names: camelCase
    fun startTracking() {
        // Implementation
    }
    
    // Constants: UPPER_SNAKE_CASE
    companion object {
        private const val DEFAULT_TIMEOUT = 5000L
    }
}
```

## 🧪 Testing

### Manual Testing
- Test on different Android versions (API 21+)
- Test on different screen sizes
- Test with different contact list sizes
- Test call tracking accuracy
- Test favorites management

### Automated Testing
We're working on expanding our test coverage. When adding new features:
- Add unit tests for business logic
- Add integration tests for complex interactions
- Update existing tests if you modify behavior

## 📱 App Architecture

DialLog follows MVVM architecture:

```
app/src/main/java/com/example/diallog002/
├── data/           # Data models, database, repositories
├── managers/       # Business logic managers
├── ui/            # Activities, fragments, adapters
└── utils/         # Utility classes and helpers
```

### Key Components
- **ContactManager**: Handles contact operations and favorites
- **CallStateMonitor**: Monitors phone call states
- **CallTracker**: Performs real-time audio analysis
- **CallLogManager**: Manages call history database

## 📋 Issue Labels

We use these labels to organize issues:

- `bug` - Something isn't working
- `enhancement` - New feature or request
- `good first issue` - Good for newcomers
- `help wanted` - Extra attention is needed
- `documentation` - Improvements or additions to documentation
- `testing` - Related to testing
- `ui/ux` - User interface and experience

## 🎯 Development Priorities

Current focus areas:
1. **Performance optimization** - Reduce battery usage
2. **UI/UX improvements** - Make the app more intuitive
3. **Testing coverage** - Add automated tests
4. **Documentation** - Improve code documentation
5. **Accessibility** - Make the app accessible to all users

## 📞 Getting Help

- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For questions and general discussion
- **Code Review**: All PRs are reviewed before merging

## 🙏 Recognition

Contributors will be:
- Added to the README contributors section
- Mentioned in release notes for significant contributions
- Given credit in the app's about section

## 📄 License

By contributing to DialLog, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing to DialLog! 🎉
