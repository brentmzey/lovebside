#!/usr/bin/env node

/**
 * ReadMe Documentation Publisher
 * 
 * This script helps publish B-Side documentation to ReadMe.com
 * 
 * Usage:
 *   node publish-to-readme.js --validate    # Validate structure
 *   node publish-to-readme.js --upload      # Upload to ReadMe
 *   node publish-to-readme.js --generate    # Generate index
 */

const fs = require('fs');
const path = require('path');

const DOCS_DIR = path.join(__dirname);

// Configuration
const config = {
  categories: [
    { slug: 'getting-started', title: 'Getting Started', order: 1 },
    { slug: 'guides', title: 'Guides', order: 2 },
    { slug: 'api-reference', title: 'API Reference', order: 3 },
    { slug: 'architecture', title: 'Architecture', order: 4 },
    { slug: 'platform-guides', title: 'Platform Guides', order: 5 },
    { slug: 'reference', title: 'Reference', order: 6 },
    { slug: 'changelog', title: 'Changelog', order: 7 }
  ]
};

// Parse frontmatter from markdown file
function parseFrontmatter(content) {
  const frontmatterRegex = /^---\n([\s\S]*?)\n---/;
  const match = content.match(frontmatterRegex);
  
  if (!match) return null;
  
  const frontmatter = {};
  const lines = match[1].split('\n');
  
  for (const line of lines) {
    const [key, ...valueParts] = line.split(':');
    if (key && valueParts.length > 0) {
      const value = valueParts.join(':').trim().replace(/^["']|["']$/g, '');
      frontmatter[key.trim()] = value;
    }
  }
  
  return frontmatter;
}

// Validate documentation structure
function validateDocs() {
  console.log('🔍 Validating documentation structure...\n');
  
  let errors = 0;
  let warnings = 0;
  
  for (const category of config.categories) {
    const categoryPath = path.join(DOCS_DIR, category.slug);
    
    if (!fs.existsSync(categoryPath)) {
      console.log(`⚠️  Warning: Category directory missing: ${category.slug}`);
      warnings++;
      continue;
    }
    
    const files = fs.readdirSync(categoryPath)
      .filter(file => file.endsWith('.md'));
    
    console.log(`📁 ${category.title} (${files.length} files)`);
    
    for (const file of files) {
      const filePath = path.join(categoryPath, file);
      const content = fs.readFileSync(filePath, 'utf-8');
      const frontmatter = parseFrontmatter(content);
      
      if (!frontmatter) {
        console.log(`   ❌ ${file}: Missing frontmatter`);
        errors++;
        continue;
      }
      
      // Validate required fields
      const required = ['title', 'excerpt', 'category', 'slug'];
      const missing = required.filter(field => !frontmatter[field]);
      
      if (missing.length > 0) {
        console.log(`   ❌ ${file}: Missing fields: ${missing.join(', ')}`);
        errors++;
      } else {
        console.log(`   ✅ ${file}`);
      }
    }
    
    console.log('');
  }
  
  console.log(`\n📊 Summary:`);
  console.log(`   Files checked: ${errors + warnings}`);
  console.log(`   Errors: ${errors}`);
  console.log(`   Warnings: ${warnings}`);
  
  if (errors > 0) {
    console.log('\n❌ Validation failed. Please fix errors before uploading.');
    process.exit(1);
  } else {
    console.log('\n✅ Validation passed!');
  }
}

// Generate documentation index
function generateIndex() {
  console.log('📝 Generating documentation index...\n');
  
  let index = '# B-Side Documentation Index\n\n';
  index += 'Auto-generated documentation structure for ReadMe.com\n\n';
  index += `Last updated: ${new Date().toISOString()}\n\n`;
  
  for (const category of config.categories) {
    const categoryPath = path.join(DOCS_DIR, category.slug);
    
    if (!fs.existsSync(categoryPath)) continue;
    
    index += `## ${category.title}\n\n`;
    
    const files = fs.readdirSync(categoryPath)
      .filter(file => file.endsWith('.md'))
      .map(file => {
        const content = fs.readFileSync(path.join(categoryPath, file), 'utf-8');
        const frontmatter = parseFrontmatter(content);
        return { file, frontmatter };
      })
      .filter(({ frontmatter }) => frontmatter)
      .sort((a, b) => (a.frontmatter.order || 99) - (b.frontmatter.order || 99));
    
    for (const { file, frontmatter } of files) {
      index += `- [${frontmatter.title}](${category.slug}/${file})\n`;
      if (frontmatter.excerpt) {
        index += `  > ${frontmatter.excerpt}\n`;
      }
    }
    
    index += '\n';
  }
  
  // Write index file
  const indexPath = path.join(DOCS_DIR, 'INDEX.md');
  fs.writeFileSync(indexPath, index);
  
  console.log(`✅ Index generated: ${indexPath}`);
}

// Upload to ReadMe (stub - requires API key)
function uploadToReadMe() {
  console.log('📤 Uploading to ReadMe...\n');
  
  const apiKey = process.env.README_API_KEY;
  
  if (!apiKey) {
    console.log('❌ Error: README_API_KEY environment variable not set');
    console.log('\nTo upload to ReadMe:');
    console.log('1. Get your API key from https://dash.readme.com/project/YOUR_PROJECT/api-key');
    console.log('2. Set environment variable: export README_API_KEY=your_api_key');
    console.log('3. Run: node publish-to-readme.js --upload');
    process.exit(1);
  }
  
  console.log('🚧 Upload functionality coming soon!');
  console.log('\nFor now, use one of these methods:');
  console.log('1. ReadMe CLI: npm install -g rdme && rdme docs . --key=$README_API_KEY');
  console.log('2. GitHub integration: Connect your repo in ReadMe dashboard');
  console.log('3. Manual upload: Copy files to ReadMe editor');
}

// Main
const args = process.argv.slice(2);
const command = args[0];

switch (command) {
  case '--validate':
    validateDocs();
    break;
  case '--generate':
    generateIndex();
    break;
  case '--upload':
    uploadToReadMe();
    break;
  default:
    console.log('ReadMe Documentation Publisher\n');
    console.log('Usage:');
    console.log('  node publish-to-readme.js --validate    Validate documentation structure');
    console.log('  node publish-to-readme.js --generate    Generate documentation index');
    console.log('  node publish-to-readme.js --upload      Upload to ReadMe.com');
    console.log('\nEnvironment variables:');
    console.log('  README_API_KEY    Your ReadMe API key (for --upload)');
}
