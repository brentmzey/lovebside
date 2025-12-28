pipeline {
    agent any

    environment {
        // Build & Test Configuration
        TEST_PB_URL = "https://bside.pockethost.io/"
        
        // Credentials IDs - THESE MUST BE CONFIGURED IN JENKINS
        // We typically use 'credentials' binding for secrets to avoid printing them
        POCKETHOST_CREDS = credentials('pockethost-ftp-creds') 
        // Assumes a UsernamePassword credential where username=user, password=pass
        
        // If FTP Host/Instance are not secrets, define them here or as params
        POCKETHOST_FTP_HOST = "ftp.pockethost.io"
        POCKETHOST_INSTANCE = "bside" 
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Setup Dependencies') {
            steps {
                sh 'chmod +x gradlew'
                // Ensure Node.js is available (tool name 'node' or just assume path)
                // If using NodeJS plugin:
                // nodejs('Node 20') { 
                    sh 'npm install' // Install ftp-deploy from package.json
                // }
            }
        }

        stage('Deploy Hooks') {
            steps {
                script {
                    // Map Jenkins credentials to environment variables expected by the script
                    // POCKETHOST_CREDS_USR and POCKETHOST_CREDS_PSW come from the binding above
                    withCredentials([usernamePassword(credentialsId: 'pockethost-ftp-creds', usernameVariable: 'POCKETHOST_FTP_USER', passwordVariable: 'POCKETHOST_FTP_PASSWORD')]) {
                        sh 'node scripts/deploy-hooks.js'
                    }
                }
            }
        }

        stage('Verify Matching Logic') {
            steps {
                script {
                    // Run specific integration test
                    sh './gradlew :shared:jvmTest --tests "love.bside.app.integration.MatchingAlgorithmTest"'
                }
            }
        }
    }

    post {
        always {
            junit 'shared/build/test-results/jvmTest/*.xml'
            archiveArtifacts artifacts: 'test_output.txt', allowEmptyArchive: true
        }
        failure {
            echo 'Matching Algorithm Verification Failed!'
        }
    }
}
