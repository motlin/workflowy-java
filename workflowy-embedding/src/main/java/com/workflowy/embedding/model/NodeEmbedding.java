package com.workflowy.embedding.model;

import java.time.Instant;

public class NodeEmbedding
{
    private final String nodeId;
    private final String model;
    private final float[] embedding;
    private final Instant systemFrom;
    private final Instant systemTo;

    public NodeEmbedding(
            String nodeId,
            String model,
            float[] embedding,
            Instant systemFrom,
            Instant systemTo)
    {
        this.nodeId = nodeId;
        this.model = model;
        this.embedding = embedding;
        this.systemFrom = systemFrom;
        this.systemTo = systemTo;
    }

    public String getNodeId()
    {
        return this.nodeId;
    }

    public String getModel()
    {
        return this.model;
    }

    public float[] getEmbedding()
    {
        return this.embedding;
    }

    public Instant getSystemFrom()
    {
        return this.systemFrom;
    }

    public Instant getSystemTo()
    {
        return this.systemTo;
    }

    public byte[] getEmbeddingAsBytes()
    {
        byte[] bytes = new byte[this.embedding.length * 4];
        for (int i = 0; i < this.embedding.length; i++)
        {
            int intBits = Float.floatToIntBits(this.embedding[i]);
            bytes[i * 4] = (byte) (intBits & 0xFF);
            bytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xFF);
            bytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xFF);
            bytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xFF);
        }
        return bytes;
    }

    public static float[] bytesToFloatArray(byte[] bytes)
    {
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++)
        {
            int intBits = (bytes[i * 4] & 0xFF)
                    | ((bytes[i * 4 + 1] & 0xFF) << 8)
                    | ((bytes[i * 4 + 2] & 0xFF) << 16)
                    | ((bytes[i * 4 + 3] & 0xFF) << 24);
            floats[i] = Float.intBitsToFloat(intBits);
        }
        return floats;
    }
}
