#!/bin/bash

# ReadMe Documentation Setup & Publishing Helper
# Usage: ./readme-setup.sh [command]

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

function print_header() {
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

function print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

function print_error() {
    echo -e "${RED}❌ $1${NC}"
}

function print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

function print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

cd "$(dirname "$0")"

case "${1:-help}" in
    
    setup)
        print_header "Setting up ReadMe Documentation"
        
        # Check if node is installed
        if ! command -v node &> /dev/null; then
            print_error "Node.js is not installed. Please install Node.js 20+"
            exit 1
        fi
        
        # Install dependencies
        print_info "Installing dependencies..."
        npm install
        
        print_success "Setup complete!"
        print_info "Next: Run './readme-setup.sh validate' to check docs"
        ;;
    
    validate)
        print_header "Validating Documentation"
        node publish-to-readme.js --validate
        ;;
    
    generate)
        print_header "Generating Documentation Index"
        node publish-to-readme.js --generate
        print_success "Index generated at INDEX.md"
        ;;
    
    preview)
        print_header "Previewing Documentation"
        print_info "This will open a local preview server"
        
        if [ ! -d "node_modules" ]; then
            print_warning "Dependencies not installed. Running setup..."
            npm install
        fi
        
        print_info "Starting preview server..."
        print_info "Open: http://localhost:3000"
        npx readme-preview
        ;;
    
    publish)
        print_header "Publishing to ReadMe.com"
        
        if [ -z "$README_API_KEY" ]; then
            print_error "README_API_KEY environment variable not set"
            echo ""
            print_info "To get your API key:"
            echo "  1. Go to https://dash.readme.com"
            echo "  2. Select your project"
            echo "  3. Go to Settings → API Keys"
            echo "  4. Create new key or copy existing"
            echo ""
            print_info "Then set the environment variable:"
            echo "  export README_API_KEY=your_api_key_here"
            echo ""
            exit 1
        fi
        
        print_info "Validating documentation..."
        node publish-to-readme.js --validate
        
        print_info "Generating index..."
        node publish-to-readme.js --generate
        
        print_info "Publishing to ReadMe..."
        npx rdme docs . --key=$README_API_KEY
        
        print_success "Documentation published!"
        ;;
    
    status)
        print_header "Documentation Status"
        
        echo ""
        echo "📁 Directory: $(pwd)"
        echo ""
        
        # Count files
        MD_COUNT=$(find . -name "*.md" | wc -l | xargs)
        echo "📄 Markdown files: $MD_COUNT"
        
        # Count lines
        LINE_COUNT=$(find . -name "*.md" -exec wc -l {} + | tail -1 | awk '{print $1}')
        echo "📝 Total lines: $LINE_COUNT"
        
        echo ""
        echo "Categories:"
        for dir in getting-started guides api-reference architecture platform-guides reference changelog; do
            if [ -d "$dir" ]; then
                count=$(find "$dir" -name "*.md" | wc -l | xargs)
                printf "  • %-20s %2d files\n" "$dir" "$count"
            fi
        done
        
        echo ""
        if [ -z "$README_API_KEY" ]; then
            print_warning "README_API_KEY not set (required for publishing)"
        else
            print_success "README_API_KEY is set"
        fi
        ;;
    
    add)
        print_header "Add New Documentation File"
        
        if [ -z "$2" ]; then
            print_error "Please specify category and filename"
            echo ""
            echo "Usage: ./readme-setup.sh add <category> <filename>"
            echo ""
            echo "Available categories:"
            echo "  - getting-started"
            echo "  - guides"
            echo "  - api-reference"
            echo "  - architecture"
            echo "  - platform-guides"
            echo "  - reference"
            echo "  - changelog"
            echo ""
            exit 1
        fi
        
        CATEGORY=$2
        FILENAME=$3
        
        if [ -z "$FILENAME" ]; then
            print_error "Please specify filename"
            exit 1
        fi
        
        # Create category if it doesn't exist
        mkdir -p "$CATEGORY"
        
        # Create file with template
        FILEPATH="$CATEGORY/$FILENAME"
        
        cat > "$FILEPATH" << 'EOF'
---
title: "Your Title Here"
excerpt: "Brief description of this page"
category: "CATEGORY_NAME"
slug: "url-slug"
order: 1
---

# Your Title

Content goes here...

## Section 1

More content...

## Section 2

Even more content...

---

> 📘 Learn More
> 
> Related links and resources
EOF
        
        # Replace category placeholder
        sed -i.bak "s/CATEGORY_NAME/$CATEGORY/" "$FILEPATH"
        rm -f "$FILEPATH.bak"
        
        print_success "Created: $FILEPATH"
        print_info "Edit the file and update the frontmatter"
        ;;
    
    clean)
        print_header "Cleaning Documentation Directory"
        
        print_info "Removing node_modules..."
        rm -rf node_modules
        
        print_info "Removing generated files..."
        rm -f INDEX.md
        
        print_success "Clean complete!"
        ;;
    
    help|*)
        print_header "ReadMe Documentation Helper"
        
        echo ""
        echo "Usage: ./readme-setup.sh [command]"
        echo ""
        echo "Commands:"
        echo "  setup       Install dependencies"
        echo "  validate    Validate documentation structure"
        echo "  generate    Generate documentation index"
        echo "  preview     Preview docs locally"
        echo "  publish     Publish to ReadMe.com (requires API key)"
        echo "  status      Show documentation statistics"
        echo "  add         Add new documentation file"
        echo "  clean       Clean generated files"
        echo "  help        Show this help"
        echo ""
        echo "Environment Variables:"
        echo "  README_API_KEY    Your ReadMe API key (for publishing)"
        echo ""
        echo "Examples:"
        echo "  ./readme-setup.sh setup"
        echo "  ./readme-setup.sh validate"
        echo "  export README_API_KEY=your_key && ./readme-setup.sh publish"
        echo "  ./readme-setup.sh add guides my-guide.md"
        echo ""
        ;;
esac
