#!/bin/bash

# Deployment script for Lyrics application
# This script builds the application and deploys it to VPS

set -e  # Exit on any error

# Configuration - Update these variables according to your setup
VPS_HOST="${VPS_HOST}"
VPS_USER="${VPS_USER}"
VPS_SSH_KEY="~/.ssh/lyrics"  # Optional: specify SSH key path
REMOTE_APP_DIR="${REMOTE_APP_DIR}"
REMOTE_SERVICE_NAME="lyrics.service"
LOCAL_ENV_FILE=".env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to execute SSH commands
ssh_exec() {
    if [ -n "$VPS_SSH_KEY" ]; then
        ssh -i "$VPS_SSH_KEY" "$VPS_USER@$VPS_HOST" "$1"
    else
        ssh "$VPS_USER@$VPS_HOST" "$1"
    fi
}

# Function to copy files via SCP
scp_copy() {
    if [ -n "$VPS_SSH_KEY" ]; then
        scp -i "$VPS_SSH_KEY" "$1" "$VPS_USER@$VPS_HOST:$2"
    else
        scp "$1" "$VPS_USER@$VPS_HOST:$2"
    fi
}

# Function to check if file exists
check_file_exists() {
    if [ ! -f "$1" ]; then
        print_error "File $1 not found!"
        exit 1
    fi
}

# Main deployment function
deploy() {
    print_info "Starting deployment process..."
    
    # Step 1: Check if .env file exists
    print_info "Checking for .env file..."
    check_file_exists "$LOCAL_ENV_FILE"
    print_success ".env file found"
    
    # Step 2: Clean and build the application
    print_info "Building application..."
    ./gradlew clean build -x test
    
    # Step 3: Find the generated JAR file
    JAR_FILE=$(find build/libs -name "*.jar" -not -name "*plain*" | head -n 1)
    if [ -z "$JAR_FILE" ]; then
        print_error "JAR file not found in build/libs!"
        exit 1
    fi
    print_success "JAR file found: $JAR_FILE"
    
    # Step 4: Create backup of current version on VPS
    print_info "Creating backup of current version..."
    ssh_exec "sudo systemctl stop $REMOTE_SERVICE_NAME || true"
    ssh_exec "sudo mkdir -p $REMOTE_APP_DIR/backup"
    ssh_exec "sudo mv -f $REMOTE_APP_DIR/lyrics.jar $REMOTE_APP_DIR/backup/ 2>/dev/null || true"
    ssh_exec "sudo mv -f $REMOTE_APP_DIR/.env $REMOTE_APP_DIR/backup/ 2>/dev/null || true"
    
    # Step 5: Copy new JAR file to VPS
    print_info "Copying JAR file to VPS..."
    scp_copy "$JAR_FILE" "/tmp/lyrics.jar"
    
    ssh_exec "sudo mv -f /tmp/lyrics.jar $REMOTE_APP_DIR/"
    ssh_exec "sudo chown $VPS_USER:$VPS_USER $REMOTE_APP_DIR/lyrics.jar"
    
    # Step 6: Copy .env file to VPS
    print_info "Copying .env file to VPS..."
    scp_copy "$LOCAL_ENV_FILE" "/tmp/.env"
    ssh_exec "sudo mv -f /tmp/.env $REMOTE_APP_DIR/"
    ssh_exec "sudo chown $VPS_USER:$VPS_USER $REMOTE_APP_DIR/.env"
    
    # Step 7: Set proper permissions
    print_info "Setting proper permissions..."
    ssh_exec "sudo chmod +x $REMOTE_APP_DIR/lyrics.jar"
    ssh_exec "sudo chmod 600 $REMOTE_APP_DIR/.env"
    
    # Step 8: Start the service
    print_info "Starting $REMOTE_SERVICE_NAME service..."
    ssh_exec "sudo systemctl start $REMOTE_SERVICE_NAME"
    ssh_exec "sudo systemctl enable $REMOTE_SERVICE_NAME"
    
    # Step 9: Check service status
    print_info "Checking service status..."
    sleep 5
    
    if ssh_exec "sudo systemctl is-active --quiet $REMOTE_SERVICE_NAME"; then
        print_success "Service $REMOTE_SERVICE_NAME is running successfully!"
    else
        print_error "Service failed to start!"
        
        print_warning "Attempting to rollback..."
        rollback
        exit 1
    fi
    
    print_success "Deployment completed successfully!"
}

# Function to rollback to previous version
rollback() {
    print_warning "Rolling back to previous version..."
    
    ssh_exec "sudo systemctl stop $REMOTE_SERVICE_NAME || true"
    
    # Check if backup exists
    if ssh_exec "test -f $REMOTE_APP_DIR/backup/lyrics.jar"; then
        ssh_exec "sudo cp $REMOTE_APP_DIR/backup/lyrics.jar $REMOTE_APP_DIR/"
        ssh_exec "sudo chown $VPS_USER:$VPS_USER $REMOTE_APP_DIR/lyrics.jar"
        ssh_exec "sudo chmod +x $REMOTE_APP_DIR/lyrics.jar"
        print_success "JAR file restored from backup"
    else
        print_warning "No backup JAR file found"
    fi
    
    # Restore .env file
    if ssh_exec "test -f $REMOTE_APP_DIR/backup/.env"; then
        ssh_exec "sudo cp $REMOTE_APP_DIR/backup/.env $REMOTE_APP_DIR/"
        ssh_exec "sudo chown $VPS_USER:$VPS_USER $REMOTE_APP_DIR/.env"
        ssh_exec "sudo chmod 600 $REMOTE_APP_DIR/.env"
        print_success ".env file restored from backup"
    else
        print_warning "No backup .env file found"
    fi
    
    ssh_exec "sudo systemctl start $REMOTE_SERVICE_NAME"
    
    if ssh_exec "sudo systemctl is-active --quiet $REMOTE_SERVICE_NAME"; then
        print_success "Rollback completed successfully!"
    else
        print_error "Rollback failed! Please check the service manually."
    fi
}



# Function to restart service
restart() {
    print_info "Restarting $REMOTE_SERVICE_NAME service..."
    ssh_exec "sudo systemctl restart $REMOTE_SERVICE_NAME"
    
    sleep 5
    if ssh_exec "sudo systemctl is-active --quiet $REMOTE_SERVICE_NAME"; then
        print_success "Service restarted successfully!"
    else
        print_error "Service failed to restart!"
    fi
}

# Function to show usage
usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  deploy    Deploy the application (default)"
    echo "  rollback  Rollback to previous version"
    echo "  restart   Restart the service"
    echo "  help      Show this help message"
    echo ""
    echo "Before running, make sure to update the configuration variables at the top of this script."
}

# Main script logic
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    rollback)
        rollback
        ;;
    restart)
        restart
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        print_error "Unknown command: $1"
        usage
        exit 1
        ;;
esac