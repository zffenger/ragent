package com.nageoffer.ai.ragent.llm.domain.service.route;

import java.util.List;

import com.nageoffer.ai.ragent.llm.domain.vo.ModelTarget;

public interface ModelSelector {

	List<ModelTarget> selectChatCandidates(boolean deepThinking);

	List<ModelTarget> selectEmbeddingCandidates();

	List<ModelTarget> selectRerankCandidates();
}
