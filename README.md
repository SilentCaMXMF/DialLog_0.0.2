
# 📱 DialLog - Smart Call Tracking & Analytics

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-3.0.0-brightgreen.svg)](CHANGELOG.md)

> Transform your phone calls into actionable insights with real-time audio analysis and communication analytics.

## 🎯 Overview

DialLog is an open-source Android application that provides intelligent call tracking and communication analytics. Perfect for sales professionals, managers, customer service representatives, and anyone looking to improve their communication skills through data-driven insights.

**Key Features:**
- 📊 **Real-time Audio Analysis** - Measures talk vs listen time during calls
- ⭐ **Smart Favorites Management** - Automatic tracking for selected contacts
- 📈 **Comprehensive Analytics** - Detailed communication patterns and insights
- 📞 **Call History Tracking** - Complete log with duration and talk/listen ratios
- 🎯 **Communication Style Analysis** - Understand your conversation patterns
- 🔒 **Privacy Focused** - All data stays on your device, no recordings made

## 🚀 Quick Start

### For Users
1. **Download**: Get the latest APK from [Releases](https://github.com/pedroocalado/DialLog/releases)
2. **Install**: Enable "Install from unknown sources" and install the APK
3. **Permissions**: Grant necessary permissions (contacts, phone, microphone)
4. **Setup**: Add your important contacts to favorites
5. **Track**: Make calls and view analytics in real-time

### For Developers
```bash
# Clone the repository
git clone https://github.com/pedroocalado/DialLog.git
cd DialLog

# Open in Android Studio
# Build and run
./gradlew assembleDebug
```

## 🛠️ How It Works

1. **Contact Selection**: Add important contacts to your favorites list
2. **Automatic Detection**: DialLog detects when you're calling a favorite contact
3. **Real-time Analysis**: During the call, it analyzes audio levels to measure:
   - Your speaking time
   - Your listening time
   - Overall conversation balance
4. **Data Collection**: Stores call duration, talk/listen ratios, and timestamps
5. **Insights Generation**: Provides analytics about your communication patterns

### 🔒 Privacy & Security
- **No Recordings**: Only audio levels are analyzed, no actual recordings are made
- **Local Storage**: All data stays on your device using Android's secure storage
- **No Cloud Sync**: No data is transmitted to external servers
- **Transparent**: Open source code available for review

## 🏗️ Architecture

```
DialLog/
├── app/src/main/java/com/example/diallog002/
│   ├── data/              # Data models and database
│   ├── managers/          # Contact and call management
│   ├── tracking/          # Call tracking and audio analysis
│   └── ui/                # Activities and UI components
├── app/src/main/res/      # Resources (layouts, drawables, etc.)
└── docs/                  # Additional documentation
```

### Key Components:
- **ContactManager**: Handles favorites and contact operations
- **CallStateMonitor**: Detects call state changes
- **CallTracker**: Performs real-time audio analysis
- **CallLogManager**: Manages call history database
- **AnalyticsActivity**: Displays communication insights

## 🔧 Technical Details

### Built With
- **Language**: Kotlin
- **UI**: Android Views (XML layouts)
- **Database**: Android Room
- **Architecture**: MVVM pattern
- **Concurrency**: Kotlin Coroutines
- **Audio**: Android AudioManager API

### Permissions Required
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
```

### System Requirements
- **Android**: 5.0 (API level 21) or higher
- **RAM**: 2GB minimum
- **Storage**: 50MB available space
- **Hardware**: Microphone access required

## 🚀 Features & Roadmap

### ✅ Current Features (v3.0.0)
- [x] Smart favorites management with contact selection
- [x] Real-time call tracking and audio analysis
- [x] Comprehensive call history with analytics
- [x] Communication style insights
- [x] Direct calling from favorites list
- [x] Automatic environment noise adaptation

### 📋 Planned Features
- [ ] Advanced analytics dashboard
- [ ] Export data functionality
- [ ] Communication coaching tips
- [ ] Team management for businesses
- [ ] Multi-language support
- [ ] Tablet optimization

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting PRs.

### Development Setup
1. **Prerequisites**: Android Studio Arctic Fox or newer, Kotlin 1.8+
2. **Clone**: `git clone https://github.com/pedroocalado/DialLog.git`
3. **Build**: Open in Android Studio and sync project
4. **Run**: Connect device/emulator and run

### Ways to Contribute
- 🐛 **Bug Reports**: Use [Issues](https://github.com/pedroocalado/DialLog/issues) to report bugs
- 💡 **Feature Requests**: Suggest new features via issues
- 🔧 **Code**: Submit pull requests with improvements
- 📚 **Documentation**: Help improve docs and comments
- 🧪 **Testing**: Help with beta testing and QA

## 📊 Beta Testing

We're actively seeking beta testers! Join our testing program to get early access to new features.

### How to Join Beta Testing:
1. **Download**: Get the latest beta APK from [Releases](https://github.com/pedroocalado/DialLog/releases)
2. **Test**: Install and test the app with your daily calls
3. **Feedback**: Report bugs and suggest improvements via [Issues](https://github.com/pedroocalado/DialLog/issues)

### What We Need Feedback On:
- User experience and interface design
- Call tracking accuracy
- Battery usage optimization
- Feature requests and suggestions
- Bug reports and crash logs

## 📈 Use Cases

### Business Applications
- **Sales**: Track conversation balance with prospects and clients
- **Customer Support**: Ensure proper listening during support calls
- **Management**: Analyze team communication patterns
- **Training**: Use data to improve communication skills

### Personal Use
- **Self-Improvement**: Understand your communication style
- **Professional Development**: Enhance meeting participation
- **Relationship Building**: Balance talking and listening

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Pedro Calado** - *Lead Developer* - [GitHub](https://github.com/pedroocalado)

## 🙏 Acknowledgments

- Android development community for resources and guidance
- Beta testers for valuable feedback and bug reports
- Open source contributors who help improve the project

## 📞 Support & Contact

- **Issues**: [GitHub Issues](https://github.com/pedroocalado/DialLog/issues)
- **Discussions**: [GitHub Discussions](https://github.com/pedroocalado/DialLog/discussions)

---

⭐ **Star this repository if you find it useful!** ⭐

**Made with ❤️ for better communication**
*Transform your conversations through self-awareness*

