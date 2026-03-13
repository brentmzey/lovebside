# ReadMe.com Publishing Quick Reference

## 🚀 Quick Setup (1-2-3)

### 1. Sign Up & Create Project
```
1. Go to https://dash.readme.com/signup
2. Create new project
3. Choose project name (e.g., "bside")
```

### 2. Get API Key
```
1. Go to Settings → API Keys
2. Create new key
3. Copy the key
```

### 3. Configure & Upload
```bash
cd readme-docs
export README_API_KEY=your_key_here
npm install
npm run deploy
```

## 📝 Document Frontmatter Template

```yaml
---
title: "Your Page Title"
excerpt: "Brief description (appears in search/lists)"
category: "getting-started"  # or guides, api-reference, etc.
slug: "url-slug"
order: 1  # Controls sort order in category
---
```

## 🎨 ReadMe Markdown Features

### Code Blocks with Tabs

```markdown
[block:code]
{
  "codes": [
    {
      "code": "npm install bside",
      "language": "shell",
      "name": "npm"
    },
    {
      "code": "yarn add bside",
      "language": "shell",
      "name": "yarn"
    }
  ]
}
[/block]
```

### Callout Boxes

```markdown
> 📘 Info
> This is an informational callout

> ❗️ Warning
> This is a warning callout

> 🚧 Under Construction
> Coming soon!

> ✅ Success
> Everything is working!
```

### Parameter Tables

```markdown
[block:parameters]
{
  "data": {
    "h-0": "Parameter",
    "h-1": "Type",
    "h-2": "Description",
    "0-0": "api_key",
    "0-1": "string",
    "0-2": "Your API key"
  },
  "cols": 3,
  "rows": 1
}
[/block]
```

## 🔧 CLI Commands

```bash
# Validate documentation structure
npm run validate

# Generate documentation index
npm run generate

# Upload to ReadMe
npm run upload

# All in one
npm run deploy
```

## 📂 Category Structure

```
getting-started/   → Tutorials, quick starts
guides/           → How-to guides  
api-reference/    → API documentation
architecture/     → System design docs
platform-guides/  → Platform-specific help
reference/        → CLI, configs, troubleshooting
changelog/        → Version history
```

## 🔗 Important URLs

| Purpose | URL |
|---------|-----|
| **Dashboard** | https://dash.readme.com |
| **Your Docs** | https://YOUR_PROJECT.readme.io |
| **API Docs** | https://docs.readme.com/reference |
| **Help** | https://docs.readme.com |
| **Status** | https://status.readme.com |

## ⚡ Quick Tasks

### Add New Document

1. Create file in appropriate category folder
2. Add frontmatter (see template above)
3. Write content with ReadMe markdown
4. Run `npm run validate`
5. Run `npm run deploy`

### Update Existing Document

1. Edit the markdown file
2. Update frontmatter if needed
3. Run `npm run validate`
4. Run `npm run deploy`

### Add Images

```markdown
![Alt text](images/screenshot.png)

# Or use ReadMe hosting
[block:image]
{
  "images": [
    {
      "image": ["URL_FROM_README_UPLOADER"],
      "caption": "Image caption"
    }
  ]
}
[/block]
```

### Add API Reference

1. Create OpenAPI/Swagger spec
2. Upload to ReadMe dashboard
3. ReadMe auto-generates interactive docs
4. Customize with guides

## 🐛 Troubleshooting

### "Document not found"
- Check slug matches filename
- Verify category exists
- Run validation script

### "Validation failed"
- Missing frontmatter fields
- Invalid YAML syntax
- Run: `npm run validate`

### "Upload failed"
- Check API key is set
- Verify network connection
- Check ReadMe status page

### "Duplicate slug"
- Each slug must be unique
- Change slug in frontmatter
- Update cross-references

## 🎯 Best Practices

### Content
✅ Start with quick start guide
✅ Include code examples
✅ Add screenshots/diagrams
✅ Keep pages focused
✅ Use callouts for important info

### Structure
✅ Logical category hierarchy
✅ Clear navigation path
✅ Proper ordering (use `order:` field)
✅ Cross-link related pages
✅ Keep docs DRY

### Maintenance
✅ Version your docs
✅ Keep changelog updated
✅ Test code examples
✅ Review regularly
✅ Get feedback

## 📊 Analytics & Insights

Enable in ReadMe Dashboard:
- Page views
- Search queries
- Popular pages
- User feedback
- Suggested edits

## 🔐 Access Control

Configure in Settings:
- **Public**: Anyone can read
- **Private**: Login required
- **Custom**: Per-page permissions

## 🎨 Customization

### Branding
- Logo upload
- Color scheme
- Custom CSS
- Favicon

### Features
- Search configuration
- Version dropdown
- Custom domain
- API key authentication

### Integrations
- GitHub (auto-sync)
- Slack (notifications)
- Analytics (Google, Segment)
- Support (Intercom, Zendesk)

## 💡 Pro Tips

1. **Use GitHub Integration** - Auto-deploy on push
2. **Version Docs** - Match app versions
3. **Test Examples** - Run all code snippets
4. **Monitor Analytics** - See what users search
5. **Collect Feedback** - Enable suggested edits
6. **SEO Optimize** - Use good titles/excerpts
7. **Keep Updated** - Docs decay quickly
8. **Link Liberally** - Help users navigate

## 📚 Resources

- **ReadMe Blog**: https://readme.com/blog
- **Community**: https://community.readme.io
- **Support**: support@readme.com
- **Status**: https://status.readme.com

---

**Your docs are in:** `/Users/brentzey/bside/readme-docs/`

**Quick check:** `cd readme-docs && npm run validate`
