// =====================================================================================
//  EcoUganda / Mukono NGO field app — Jenkins Multibranch Pipeline
//
//  Every branch and pull request with this file gets its own job automatically.
//    PR-*        : unit tests + lint + debug APK (fast; perf bytecode instrumentation off)
//    feature/*   : same as PR
//    develop     : + APK with Firebase Performance instrumentation
//    main        : + upload to Firebase App Distribution (field-volunteers, warehouse-tablets)
//
//  Required on the agent  : label "android", JDK 21, Android SDK (ANDROID_HOME set)
//  Required credentials   : ecouganda-google-services-json (Secret file),
//                           ecouganda-firebase-service-account (Secret file, only for Distribute / Test Lab)
// =====================================================================================

pipeline {
    agent { label 'android' }

    tools {
        jdk 'jdk21'   // Manage Jenkins > Tools > JDK installations, name must match
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds(abortPrevious: true)
    }

    parameters {
        booleanParam(name: 'DISTRIBUTE', defaultValue: false,
                description: 'Upload the debug APK to Firebase App Distribution (always on for main)')
        booleanParam(name: 'RUN_TEST_LAB', defaultValue: false,
                description: 'Robo test on Firebase Test Lab physical devices to generate Performance data')
        string(name: 'TEST_LAB_DEVICE', defaultValue: '',
                description: 'Physical low-end device for Test Lab, e.g. model=<id>,version=<api> (gcloud firebase test android models list)')
        string(name: 'NETWORK_PROFILE', defaultValue: '',
                description: 'Test Lab network profile ID (see: gcloud firebase test network-profiles list). Empty = none')
    }

    environment {
        // Pull requests skip the perf plugin's bytecode instrumentation: faster CI, same unit tests.
        PERF_INSTRUMENTATION = "${env.CHANGE_ID ? 'false' : 'true'}"
    }

    stages {

        stage('Environment') {
            steps {
                gradlew('--version')
            }
        }

        stage('Unit Tests') {
            steps {
                withFirebaseConfig {
                    gradlew("testDebugUnitTest -PfirebasePerformanceInstrumentationEnabled=${env.PERF_INSTRUMENTATION}")
                }
            }
            post {
                always {
                    junit testResults: 'app/build/test-results/testDebugUnitTest/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Lint') {
            steps {
                withFirebaseConfig {
                    gradlew("lintDebug -PfirebasePerformanceInstrumentationEnabled=${env.PERF_INSTRUMENTATION}")
                }
            }
            post {
                always {
                    recordIssues(
                            tools: [androidLintParser(pattern: 'app/build/reports/lint-results-debug.xml')],
                            qualityGates: [[threshold: 1, type: 'TOTAL_ERROR', criticality: 'UNSTABLE']])
                }
            }
        }

        stage('Build APK') {
            steps {
                withFirebaseConfig {
                    gradlew("assembleDebug -PfirebasePerformanceInstrumentationEnabled=${env.PERF_INSTRUMENTATION}")
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk', fingerprint: true
                }
            }
        }

        stage('Firebase Test Lab (optional)') {
            when {
                allOf {
                    not { changeRequest() }
                    expression { params.RUN_TEST_LAB }
                }
            }
            steps {
                withCredentials([file(credentialsId: 'ecouganda-firebase-service-account', variable: 'GCP_SA')]) {
                    script {
                        if (!params.TEST_LAB_DEVICE?.trim()) {
                            error('Set TEST_LAB_DEVICE, e.g. model=<id>,version=<api> from: gcloud firebase test android models list')
                        }
                        String profile = params.NETWORK_PROFILE?.trim() ? "--network-profile=${params.NETWORK_PROFILE.trim()}" : ''
                        String keyFile = isUnix() ? '"$GCP_SA"' : '"%GCP_SA%"'
                        cmd("gcloud auth activate-service-account --key-file=${keyFile}")
                        cmd("gcloud firebase test android run --type robo " +
                            "--app app/build/outputs/apk/debug/app-debug.apk " +
                            "--device ${params.TEST_LAB_DEVICE.trim()} --timeout 5m ${profile}")
                    }
                }
            }
        }

        stage('Distribute to field devices') {
            when {
                allOf {
                    not { changeRequest() }
                    anyOf { branch 'main'; expression { params.DISTRIBUTE } }
                }
            }
            steps {
                withCredentials([
                        file(credentialsId: 'ecouganda-google-services-json', variable: 'GOOGLE_SERVICES_JSON'),
                        file(credentialsId: 'ecouganda-firebase-service-account', variable: 'FIREBASE_APP_DIST_SA')]) {
                    gradlew('appDistributionUploadDebug')
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'app/build/reports/**', allowEmptyArchive: true
        }
        cleanup {
            cleanWs()   // also removes the copied google-services.json
        }
    }
}

// ---- helpers (work on Linux and Windows agents) -------------------------------------

def gradlew(String args) {
    if (isUnix()) {
        sh 'chmod +x gradlew'
        sh "./gradlew --no-daemon --stacktrace ${args}"
    } else {
        bat "gradlew.bat --no-daemon --stacktrace ${args}"
    }
}

def cmd(String line) {
    if (isUnix()) { sh line } else { bat line }
}

def withFirebaseConfig(Closure body) {
    withCredentials([file(credentialsId: 'ecouganda-google-services-json', variable: 'GOOGLE_SERVICES_JSON')]) {
        body()
    }
}
