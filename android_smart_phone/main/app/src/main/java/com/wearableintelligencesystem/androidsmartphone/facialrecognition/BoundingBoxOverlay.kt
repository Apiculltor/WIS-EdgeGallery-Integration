package com.wearableintelligencesystem.androidsmartphone.facialrecognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.View

class BoundingBoxOverlay(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    // Propriedades necessárias para o funcionamento
    private var faceBoundingBoxes: List<FaceBox>? = null
    private var areOimsInit = false
    private var frameWidth = 0
    private var frameHeight = 0
    private val outputOverlayTransform = Matrix()

    // Paint para desenhar as caixas delimitadoras
    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    // Data class para representar uma face box
    data class FaceBox(
        val bbox: RectF,
        val label: String = ""
    )

    // Métodos para configurar os dados
    fun setFaceBoundingBoxes(boxes: List<FaceBox>?) {
        faceBoundingBoxes = boxes
        invalidate()
    }

    fun setFrameSize(width: Int, height: Int) {
        frameWidth = width
        frameHeight = height
        areOimsInit = false
    }

    fun clearBoundingBoxes() {
        faceBoundingBoxes = null
        areOimsInit = false
        invalidate()
    }

    // Método surfaceDestroyed (se esta classe implementa SurfaceHolder.Callback)
    // Se não implementa, remova este método completamente
    fun surfaceDestroyed(holder: SurfaceHolder) {
        // Implementação específica se necessário
        clearBoundingBoxes()
    }

    // CORREÇÃO: Método onDraw correto - removendo override que estava causando erro
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Usando canvas diretamente (não nullable na assinatura correta)
        faceBoundingBoxes?.let { boxes ->
            if (!areOimsInit) {
                initializeTransformations(canvas)
            } else {
                drawFaceBoundingBoxes(canvas, boxes)
            }
        }
    }

    private fun initializeTransformations(canvas: Canvas) {
        if (frameWidth > 0 && frameHeight > 0) {
            val viewWidth = canvas.width.toFloat()
            val viewHeight = canvas.height.toFloat()
            val xFactor: Float = viewWidth / frameWidth.toFloat()
            val yFactor: Float = viewHeight / frameHeight.toFloat()

            // Scale and mirror coordinates (required for front lens)
            outputOverlayTransform.reset()
            outputOverlayTransform.preScale(xFactor, yFactor)
            outputOverlayTransform.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
            areOimsInit = true
        }
    }

    private fun drawFaceBoundingBoxes(canvas: Canvas, boxes: List<FaceBox>) {
        for (face in boxes) {
            val boundingBox = RectF(face.bbox)
            outputOverlayTransform.mapRect(boundingBox)
            canvas.drawRoundRect(boundingBox, 16f, 16f, boxPaint)
            // Removendo drawText vazio que estava causando problemas
        }
    }
}