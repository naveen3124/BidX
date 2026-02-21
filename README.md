# BidX
A Index on Bengaluru

## Run With Docker

### 1. Build image
```bash
docker build -t bidx-rss:latest .
```

### 2. Run container (persist index on host)
Use a host bind mount so Lucene index data is not lost when the container is removed.

```bash
docker run -d ^
  --name bidx-rss ^
  -p 8080:8080 ^
  -v C:\Users\navee\Bidx\BidX\data\lucene-index:/data/lucene-index ^
  bidx-rss:latest
```

### 3. Health check
```bash
curl http://localhost:8080/health
```
Expected response: `OK`

### 4. Useful container commands
```bash
# View running containers
docker ps

# View logs
docker logs -f bidx-rss

# Stop container
docker stop bidx-rss

# Start container again
docker start bidx-rss

# Remove container (volume data remains if bind-mounted or named volume is kept)
docker rm bidx-rss

# Stop and remove in one command
docker rm -f bidx-rss
```

### Optional: use a named Docker volume
```bash
docker run -d --name bidx-rss -p 8080:8080 -v rss_index_data:/data/lucene-index bidx-rss:latest
```

### Volume commands (named volume)
```bash
# List volumes
docker volume ls

# Inspect volume details
docker volume inspect rss_index_data

# Check files inside the volume
docker run --rm -v rss_index_data:/data alpine ls -la /data

# Remove volume (only when you want to delete indexed data)
docker volume rm rss_index_data
```

### Bind mount check (host path)
If you used the bind mount option, index files are directly available at:
`C:\Users\navee\Bidx\BidX\data\lucene-index`
