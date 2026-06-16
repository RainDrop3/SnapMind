package com.example.snapmind.core.ai

import com.example.snapmind.data.local.entity.ClassificationEntity
import com.example.snapmind.data.local.entity.TagAssignedBy
import com.example.snapmind.data.local.entity.TagAssignmentSource
import com.example.snapmind.data.local.entity.VisionLabelEntity
import com.example.snapmind.data.repository.TagAssignmentRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoTagRuleEngine @Inject constructor() {

    /**
     * 자동 태그는 **TFLite 분류 모델의 최상위(top-1) 결과 단 하나만** 사용한다.
     * 모델은 8개 카테고리만 출력하며, confidence가 임계값(0.65) 미만이면
     * ImageClassifier가 top-1 라벨을 "Unknown"으로 치환해 넘겨준다. 따라서 여기서는
     * 별도의 시드 태그나 Vision·OCR 파생 태그를 붙이지 않는다.
     * (파라미터 시그니처는 호출부 호환을 위해 유지한다.)
     */
    fun buildAssignments(
        ocrText: String?,
        classifications: List<ClassificationEntity>,
        visionLabels: List<VisionLabelEntity>,
    ): List<TagAssignmentRequest> {
        val top = classifications
            .filter { it.label.isNotBlank() }
            .minByOrNull { it.rank }
            ?: return emptyList()
        return listOf(
            TagAssignmentRequest(
                rawName = top.label.trim().lowercase(),
                assignedBy = TagAssignedBy.AUTO,
                sources = setOf(TagAssignmentSource.TFLITE),
            ),
        )
    }
}
