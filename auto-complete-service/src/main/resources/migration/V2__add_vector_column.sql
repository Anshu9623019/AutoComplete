-- Step 1: Drop the old index
DROP INDEX IF EXISTS idx_embedding_hnsw;

-- Step 2: Change column from vector(1536) to vector(384)
ALTER TABLE query_frequency
    ALTER COLUMN embedding TYPE vector(384)
    USING NULL::vector(384);

-- Step 3: Recreate index with correct dimensions
CREATE INDEX idx_embedding_hnsw
ON query_frequency
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);