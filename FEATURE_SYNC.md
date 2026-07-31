# Feature Sync Tracking

## Overview
This document tracks which features from the main zentro-rajkot repository have been synced to the zentro-server-local fork.

## Main Repository
- **Repository**: https://github.com/Nayan910/zentro-rajkot
- **Last Synced**: [Date]

## Features Already Synced
- [x] User authentication (JWT)
- [x] User registration and login
- [x] Gig creation and management
- [x] Gig applications
- [x] Basic chat functionality
- [x] Activity logging
- [x] WebSocket support

## Features Pending Sync
- [ ] Global chat room (NEW - added in fork)
- [ ] User search by username (NEW - added in fork)
- [ ] Private chat (NEW - added in fork)
- [ ] Chat list view (NEW - added in fork)

## How to Sync New Features

### Method 1: Manual Sync
1. Check the main repository for new commits
2. Review changes and identify relevant features
3. Manually implement changes in the fork
4. Test thoroughly before committing

### Method 2: Git Remote Add (Recommended)
```bash
# Add main repo as upstream
git remote add upstream https://github.com/Nayan910/zentro-rajkot.git

# Fetch upstream changes
git fetch upstream

# Merge upstream changes (careful with conflicts)
git merge upstream/main

# Resolve any conflicts and test
```

### Method 3: Cherry-Pick Specific Commits
```bash
# Fetch upstream
git fetch upstream

# Cherry-pick specific commit
git cherry-pick <commit-hash>
```

## Sync Process
1. **Review**: Check main repo for new features/fixes
2. **Assess**: Determine if feature is relevant to server version
3. **Implement**: Add feature to fork with server-side modifications
4. **Test**: Verify all functionality works
5. **Document**: Update this file with synced features

## Important Notes
- Server version uses H2 database (no Firebase)
- All real-time features use WebSocket/STOMP
- Keep package name: com.skillmatch.rajkot
- Maintain consistent design system
- Test server endpoints before Android integration

## Recent Sync Activity
- [Date] - Added global chat, user search, private chat features
- [Date] - Initial fork created from zentro-rajkot
