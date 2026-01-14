package com.workflowy.embedding.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.workflowy.embedding.model.EmbeddingModel;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnnxEmbeddingEngine implements EmbeddingEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(OnnxEmbeddingEngine.class);

	private final EmbeddingModel model;
	private final String modelCachePath;
	private ZooModel<String, float[]> zooModel;
	private Predictor<String, float[]> predictor;

	public OnnxEmbeddingEngine(EmbeddingModel model, String modelCachePath) {
		if (!model.isLocal()) {
			throw new IllegalArgumentException("Model must be a local ONNX model: " + model);
		}

		this.model = model;
		this.modelCachePath = modelCachePath;

		this.initializeModel();
	}

	private void initializeModel() {
		try {
			Path cachePath = Paths.get(this.modelCachePath);
			System.setProperty("DJL_CACHE_DIR", cachePath.toString());

			Criteria<String, float[]> criteria = Criteria.builder()
				.setTypes(String.class, float[].class)
				.optApplication(Application.NLP.TEXT_EMBEDDING)
				.optEngine("OnnxRuntime")
				.optModelUrls("djl://ai.djl.huggingface.onnxruntime/" + this.model.getModelName())
				.optTranslator(new SentenceTransformerTranslator(this.model))
				.build();

			this.zooModel = criteria.loadModel();
			this.predictor = this.zooModel.newPredictor();

			LOGGER.info("ONNX model loaded: {}", this.model.getModelName());
		} catch (ModelNotFoundException | MalformedModelException | IOException e) {
			throw new RuntimeException("Failed to load ONNX model: " + this.model.getModelName(), e);
		}
	}

	@Override
	public float[] generateEmbedding(String text, boolean isQuery) {
		String prefixedText = this.applyPrefix(text, isQuery);

		try {
			return this.predictor.predict(prefixedText);
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate embedding", e);
		}
	}

	@Override
	public List<float[]> generateEmbeddings(List<String> texts, boolean isQuery) {
		MutableList<float[]> embeddings = Lists.mutable.empty();
		for (String text : texts) {
			embeddings.add(this.generateEmbedding(text, isQuery));
		}
		return embeddings;
	}

	private String applyPrefix(String text, boolean isQuery) {
		String prefix = isQuery ? this.model.getQueryPrefix().orElse("") : this.model.getPassagePrefix().orElse("");
		return prefix + text;
	}

	@Override
	public EmbeddingModel getModel() {
		return this.model;
	}

	@Override
	public void close() {
		if (this.predictor != null) {
			this.predictor.close();
		}
		if (this.zooModel != null) {
			this.zooModel.close();
		}
	}

	private static class SentenceTransformerTranslator implements Translator<String, float[]> {

		private final EmbeddingModel model;
		private HuggingFaceTokenizer tokenizer;

		public SentenceTransformerTranslator(EmbeddingModel model) {
			this.model = model;
		}

		@Override
		public void prepare(TranslatorContext ctx) {
			try {
				this.tokenizer = HuggingFaceTokenizer.newInstance(this.model.getModelName());
			} catch (Exception e) {
				throw new RuntimeException("Failed to load tokenizer", e);
			}
		}

		@Override
		public NDList processInput(TranslatorContext ctx, String input) {
			Encoding encoding = this.tokenizer.encode(input);

			NDManager manager = ctx.getNDManager();
			long[] inputIds = encoding.getIds();
			long[] attentionMask = encoding.getAttentionMask();

			NDArray inputIdsArray = manager.create(new long[][] { inputIds });
			NDArray attentionMaskArray = manager.create(new long[][] { attentionMask });

			return new NDList(inputIdsArray, attentionMaskArray);
		}

		@Override
		public float[] processOutput(TranslatorContext ctx, NDList list) {
			NDArray lastHiddenState = list.get(0);

			NDArray meanPooled = lastHiddenState.mean(new int[] { 1 });

			NDArray normalized = meanPooled.div(meanPooled.norm());

			return normalized.toFloatArray();
		}
	}
}
