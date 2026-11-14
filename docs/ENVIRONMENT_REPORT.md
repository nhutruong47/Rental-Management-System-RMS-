# Environment report

## Repository discovery

- Intended repository URL was confirmed from the user: `https://github.com/nhutruong47/Rental-Management-System-RMS-`.
- GitHub API reports the public repository exists, created 2026-11-13, default branch `main`, size 0 bytes. `git ls-remote` returned no refs; clone reported an empty repository.
- Worktree: `/home/ubuntu/Rental-Management-System-RMS-`. Fresh clone, initially only `.git`, branch `main`, no source, tests, docs, status files, or inherited changes. No `AGENTS.md` found.
- A similarly named workspace guess was rejected: `/home/ubuntu/hotel` is Nhu Villas and was not modified.

## Host and tooling observed

- Linux `6.17.0-1011-oracle`, `aarch64`, glibc 2.39.
- Git, Docker Engine 29.1.3 and Docker Compose 2.40.3 available; daemon responds.
- Docker image `eclipse-temurin:21-jdk` was pulled and `java -version` executed successfully (Temurin Java 21.0.13 LTS). Java/Javac/Maven are not installed on host; container builds are the intended safe alternative.
- Node.js 26.8.2 and npm 11.19.1 are available; project target will be pinned to a compatible LTS instead of relying on host version.
- PostgreSQL client (`psql`) not installed on host. PostgreSQL 15 and 16 Docker images are locally available.
- Approx. 11 GiB RAM reported (4.0 GiB available at discovery), no swap; 48 GiB root filesystem with 8.5 GiB available at discovery (83% used).
- `gh` CLI is unavailable. No dependency is installed globally and no public deployment is attempted.

## Execution constraints and verification plan

- Build/test Java and PostgreSQL in local Docker containers; use Compose for integrated development if available disk permits.
- Avoid pulling large runtime images unnecessarily; reuse locally available PostgreSQL where practical.
- Disk space is a material limit; inspect it before adding images or installing large dependencies.
- No business workflow has passed at baseline: the remote repository was empty. Record each real test command, count/status and limitation in `TEST_RESULTS.md` after execution.
