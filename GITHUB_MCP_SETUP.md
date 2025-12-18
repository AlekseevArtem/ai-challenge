# GitHub MCP Server Setup Guide

## Overview

This project now integrates GitHub's official MCP server, providing 100+ tools for repository management, issues, PRs, CI/CD monitoring, and more.

## Prerequisites

- Docker installed and running
- GitHub account
- GitHub Personal Access Token (PAT)

## Setup Steps

### 1. Create GitHub Personal Access Token

1. Go to GitHub Settings: https://github.com/settings/tokens
2. Click "Generate new token" → "Generate new token (classic)"
3. Name: `MCP Server` (or any name you prefer)
4. Select scopes (minimum required):
   - `repo` (Full control of private repositories)
   - `read:org` (Read org and team membership)
   - `read:user` (Read user profile data)
   - `read:project` (Read project data)

   **Note**: For more features, you may want to add:
   - `workflow` (Update GitHub Actions workflows)
   - `admin:repo_hook` (Full control of repository hooks)
   - `notifications` (Access notifications)

5. Click "Generate token"
6. **Copy the token immediately** (you won't be able to see it again)

### 2. Set Environment Variable

**Option A: Temporary (current session only)**
```bash
export GITHUB_PERSONAL_ACCESS_TOKEN="ghp_your_token_here"
```

**Option B: Persistent (recommended)**

Add to your shell profile (`~/.zshrc` for macOS default, or `~/.bashrc`):
```bash
echo 'export GITHUB_PERSONAL_ACCESS_TOKEN="ghp_your_token_here"' >> ~/.zshrc
source ~/.zshrc
```

**Option C: IntelliJ IDEA Run Configuration**
1. Edit Run Configuration for your server
2. Add environment variable: `GITHUB_PERSONAL_ACCESS_TOKEN=ghp_your_token_here`

### 3. Verify Docker Image

The image should already be pulled:
```bash
docker images | grep github-mcp-server
```

Expected output:
```
ghcr.io/github/github-mcp-server   latest   ...   ...
```

### 4. Test GitHub MCP Server Manually (Optional)

Test the Docker container directly:
```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | docker run -i --rm -e GITHUB_PERSONAL_ACCESS_TOKEN=$GITHUB_PERSONAL_ACCESS_TOKEN ghcr.io/github/github-mcp-server
```

You should see a JSON response with capabilities.

### 5. Start Your Server

The GitHub MCP client is already configured in `AppModule.kt` and will auto-register when:
1. Docker is running
2. `GITHUB_PERSONAL_ACCESS_TOKEN` environment variable is set

Start your server:
```bash
./gradlew :server:run
```

Look for these log messages:
```
Found GitHub token, registering GitHub MCP client
Registered GitHub MCP client
```

## Integration Details

### Architecture

```
User Chat Message
       ↓
ClaudeApiService
       ↓
MCPManager (routes tool calls)
       ↓
GitHub MCPClient
       ↓
Docker Container (GitHub MCP Server)
       ↓
GitHub REST API
```

### Configuration Location

- **Main Config**: `server/src/main/kotlin/ru/alekseev/myapplication/di/AppModule.kt` (lines 123-145)
- **Docker Command**: Uses `-i` (interactive) and `--rm` (auto-remove)
- **Environment**: Token passed via `-e` flag to Docker

### Available Tools

Once connected, Claude will have access to 100+ GitHub tools including:

**Repository Tools**:
- `get_file_contents` - Read file contents
- `search_code` - Search code in repositories
- `list_commits` - List commit history
- `get_repository` - Get repository metadata
- `list_repositories` - List user/org repositories

**Issue & PR Tools**:
- `create_issue` - Create new issue
- `list_issues` - List issues with filters
- `create_pull_request` - Create PR
- `list_pull_requests` - List PRs
- `merge_pull_request` - Merge PR

**CI/CD Tools**:
- `list_workflows` - List GitHub Actions workflows
- `list_workflow_runs` - List workflow run history
- `get_workflow_run` - Get run details

**And many more** for branches, releases, gists, projects, etc.

## Usage Example

Once set up, you can ask Claude:

```
"Go to https://github.com/anthropics/anthropic-sdk-python, analyze the repository and save a summary to my reminders"
```

Claude will:
1. Detect the GitHub URL
2. Autonomously call GitHub tools (get_repository, get_file_contents, etc.)
3. Summarize the findings using LLM
4. Save to Reminder MCP tool

## Troubleshooting

### Error: "GITHUB_PERSONAL_ACCESS_TOKEN not found"
- Verify the token is set: `echo $GITHUB_PERSONAL_ACCESS_TOKEN`
- Make sure you restarted your terminal after adding to shell profile
- Check IntelliJ run configuration if running from IDE

### Error: "Cannot connect to Docker daemon"
- Start Docker Desktop
- Verify: `docker ps`

### Error: "rate limit exceeded"
- Check your token scopes include `repo`
- Wait for rate limit reset (check headers in GitHub API responses)
- Consider upgrading GitHub plan for higher limits

### Docker image not found
```bash
docker pull ghcr.io/github/github-mcp-server:latest
```

### MCP connection timeout
- Check Docker logs: `docker logs $(docker ps -q --filter ancestor=ghcr.io/github/github-mcp-server)`
- Verify token has correct permissions
- Check network connectivity

## Security Notes

- **Never commit tokens**: Add `.env` files to `.gitignore`
- **Use fine-grained tokens** when possible for better security
- **Rotate tokens regularly**: Generate new ones every 90 days
- **Minimal scopes**: Only grant permissions you need
- **Revoke unused tokens**: Clean up old tokens at https://github.com/settings/tokens

## References

- [GitHub MCP Server Official Repo](https://github.com/github/github-mcp-server)
- [GitHub PAT Documentation](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
- [Model Context Protocol Spec](https://modelcontextprotocol.io/)
