# CI/CD Pipeline

## Purpose

The CI/CD pipeline is used to automatically build, test and deploy the Sgula website when changes are pushed to GitHub.

## Tools used

- GitHub — source code and version control
- GitHub Actions — CI/CD automation
- Google Cloud Build — builds the Docker image
- Artifact Registry — stores the Docker image
- Cloud Run — hosts the ASP.NET Core website
- Firebase Hosting — provides the production web address
- Firebase CLI — deploys Firebase resources

## Pipeline

```text
Developer
    ↓
Git Push
    ↓
GitHub
    ↓
GitHub Actions
    ↓
Build Docker Image
    ↓
Artifact Registry
    ↓
Deploy to Cloud Run
    ↓
Deploy Firebase Resources
    ↓
Production Website