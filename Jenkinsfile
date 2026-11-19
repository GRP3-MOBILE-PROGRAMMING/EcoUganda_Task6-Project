// =====================================================================================
//  EcoUganda / Mukono NGO field app — Jenkins Multibranch Pipeline
//
//  Every branch and pull request with this file gets its own job automatically.
//    PR-*        : unit tests + lint + debug APK (fast; perf bytecode instrumentation off)
//    feature/*   : same as PR
//    develop     : + APK with Firebase Performance instrumentation
//    main        : + Firebase Test Lab device matrix (smoke) + Robo crawl
//                  + upload to Firebase App Distribution (field-volunteers, warehouse-tablets)
//
//  Required on the agent  : label "android", JDK 21, Android SDK (ANDROID_HOME set)
//  Required credentials   : ecouganda-google-services-json (Secret file),
//                           ecouganda-firebase-service-account (Secret file, only for Distribute / Test Lab)
//  Test Lab also needs   : Google Cloud SDK (gcloud) on the agent PATH
// =====================================================================================

pipeline {
    agent { label 'android' }

    tools {
        jdk 'JDK21'   // Manage Jenkins > Tools > JDK installations, name must match
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
        timestamps()
        timeout(time: 75, unit: 'MINUTES')   // Test Lab matrices take 10-25 min
        disableConcurrentBuilds(abortPrevious: true)
    }

    parameters {
        booleanParam(name: 'DISTRIBUTE', defaultValue: false,
                description: 'Upload the debug APK to Firebase App Distribution (always on for main)')
        booleanParam(name: 'RUN_TEST_LAB', defaultValue: false,
                description: 'Run the Firebase Test Lab device tests on this branch (always on for main)')
        choice(name: 'TEST_LAB_MATRIX', choices: ['smoke', 'full'],
                description: 'smoke = 3 devices (daily budget); full = 7 devices incl. landscape starts (nightly/release)')
        booleanParam(name: 'RUN_ROBO', defaultValue: true,
                description: 'Also run the Robo crawl (2 devices) for crashes, ANRs and UI issues')
    }

    environment {
        // Pull requests skip the perf plugin's bytecode instrumentation: faster CI, same unit tests.
        PERF_INSTRUMENTATION = "${env.CHANGE_ID ? 'false' : 'true'}"
        FIREBASE_PROJECT = 'ecouganda'
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
                    // app APK + the instrumented test APK used by Test Lab (also proves the device tests compile)
                    gradlew("assembleDebug assembleDebugAndroidTest -PfirebasePerformanceInstrumentationEnabled=${env.PERF_INSTRUMENTATION}")
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk, app/build/outputs/apk/androidTest/debug/*.apk',
                            fingerprint: true
                }
            }
        }

        stage('Firebase Test Lab') {
            when {
                allOf {
                    not { changeRequest() }
                    anyOf { branch 'main'; expression { params.RUN_TEST_LAB } }
                }
            }
            steps {
                withCredentials([file(credentialsId: 'ecouganda-firebase-service-account', variable: 'GCP_SA')]) {
                    script {
                        String keyFile = isUnix() ? '"$GCP_SA"' : '"%GCP_SA%"'
                        String matrix = params.TEST_LAB_MATRIX ?: 'smoke'
                        cmd("gcloud auth activate-service-account --key-file=${keyFile}")
                        cmd("gcloud config set project ${env.FIREBASE_PROJECT}")

                        // Instrumented tests: drawer, responsive layout, deep links, rotation
                        catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                            cmd("gcloud firebase test android run testlab/testlab.yml:${matrix}")
                        }
                        // Robo crawl: crashes, ANRs, UI issues (runs even if the suite above failed)
                        if (params.RUN_ROBO == null || params.RUN_ROBO) {
                            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                                cmd('gcloud firebase test android run testlab/testlab.yml:robo')
                            }
                        }
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
