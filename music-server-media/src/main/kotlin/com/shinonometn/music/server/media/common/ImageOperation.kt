package com.shinonometn.music.server.media.common

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

class ImageOperation(private val bufferedImage: BufferedImage, builder: ImageOperation.() -> Unit) {
    var width = bufferedImage.width
        private set

    var height = bufferedImage.height
        private set

    var targetWidth = width
        private set

    var targetHeight = height
        private set

    var x = 0
        private set

    var y = 0
        private set

    var backgroundColor: Color = Color.black

    fun scaleFitToWidth(targetWidth: Int) {
        val height = bufferedImage.height
        val width = bufferedImage.width

        this.width = targetWidth
        this.height = (targetWidth * (height / width.toDouble())).roundToInt()

        this.targetWidth = targetWidth
    }

    fun scaleFitToHeight(targetHeight: Int) {
        val width = bufferedImage.width
        val height = bufferedImage.height

        this.width = (targetHeight * (width / height.toDouble())).roundToInt()
        this.height = targetHeight

        this.targetHeight = targetHeight
    }

    fun crop(targetWidth: Int, targetHeight: Int) {
        this.targetWidth = targetWidth
        this.targetHeight = targetHeight
    }

    fun scaleKeepRatio(targetWidth: Int, targetHeight: Int) {
        if (targetWidth == width && targetHeight == height) return
        if (targetHeight >= targetWidth) scaleFitToHeight(targetHeight) else scaleFitToWidth(targetWidth)
        crop(targetWidth, targetHeight)
    }

    fun alignLeft() {
        this.x = 0
    }

    fun alignRight() {
        this.x = targetWidth - width
    }

    fun alignTop() {
        this.y = 0
    }

    fun alignBottom() {
        this.y = targetHeight - height
    }

    fun alignCenter() {
        this.x = ((targetWidth.toDouble() - width) / 2.0).roundToInt()
        this.y = ((targetHeight.toDouble() - height) / 2.0).roundToInt()
    }

    init {
        builder()
    }

    fun toImage(): BufferedImage {
        val source = bufferedImage.getScaledInstance(width, height, BufferedImage.SCALE_SMOOTH)
        val image = BufferedImage(targetWidth, targetHeight, bufferedImage.type)
        val graphics2D = image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        }
        graphics2D.color = backgroundColor
        graphics2D.fillRect(0, 0, targetWidth, targetHeight)
        graphics2D.drawImage(source, x, y, null)
        graphics2D.dispose()
        return image
    }
}