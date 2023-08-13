pipeline {
  agent any

  stages {
    stage("Checkout Code") {
      parallel {
        stage("Checkout Flask Code") {
          steps {
            dir("${env.WORKSPACE}/flask") {
              git branch: 'main', credentialsId: 'git_credentials', url: 'http://sber_git/emils/flask_hello_world.git'
            }
          }
        }
        stage("Checkout FastAPI Code") {
          steps {
            dir("${env.WORKSPACE}/fastapi") {
              git branch: 'main', credentialsId: 'git_credentials', url: 'http://sber_git/emils/fastapi_hello_world.git'
            }
          }
        }
      }
    }
    stage("Build") {
      parallel {
        stage("Build Flask App") {
          agent {
            dockerfile {
              filename 'Dockerfile'
              customWorkspace "${env.WORKSPACE}/flask"
              additionalBuildArgs '-t flask-app:latest'
            }
          }
          steps {
            echo "Building the Flask App.."
          }
        }
        stage("Build FastAPI App") {
          agent {
            dockerfile {
              filename 'Dockerfile'
              customWorkspace "${env.WORKSPACE}/fastapi"
              additionalBuildArgs '-t fastapi-app:latest'
            }
          }
          steps {
            echo "Building the FastAPI App.."
          }
        }
      }
    }
    stage("Deploy") { 
      parallel {
        stage("Deploy Flask App") {
          agent {
            docker {
              image 'flask-app:latest'
            }
          }
          steps {
            echo "Flask App deployed"
          }
        }
        stage("Deploy FastAPI App") {
          agent {
            docker {
              image 'fastapi-app:latest'
            }
          }
          steps {
            echo "FastAPI App deployed"
          }
        }
      }
    }
  }
  post { 
    success {
      sh "docker stop flask_app fastapi_app || true"
      sh "docker container remove flask_app fastapi_app || true"
      sh "docker run -d -p 8083:5000 --name flask_app --network sber_homework_default flask-app:latest"
      sh "docker run -d -p 8084:8000 --name fastapi_app --network sber_homework_default fastapi-app:latest"
      sh "sleep 5"
      sh "curl -X 'GET' 'http://flask_app:5000/'"
      sh "curl -X 'GET' 'http://fastapi_app:8000/name'"
      sh "docker ps | grep -P 'flask|fastapi|NAMES'"
    }
  }
}
