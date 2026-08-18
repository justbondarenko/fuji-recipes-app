package dev.bondarenko.fujirecipes.ui.theme.icons

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icon stroke weight presets for Material Symbols Rounded.
 */
enum class IconWeight(val weight: Int) {
    W300(300),
    W400(400),
    W500(500),
}

/**
 * Global icon configuration.
 * Modifying [weight] immediately updates all icons across the entire app via Compose state.
 */
object FujiIconConfig {
    var weight: IconWeight by mutableStateOf(IconWeight.W300)
}

/**
 * Unified Material Symbols Rounded icons for Fuji Recipes app.
 */
object FujiIcons

// Aliases for convenience
public val FujiIcons.Clear: ImageVector get() = CloseSmall
public val FujiIcons.Close: ImageVector get() = CloseSmall
public val FujiIcons.StarBorder: ImageVector get() = Star

public val FujiIcons.Add: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _addW300 ?: ImageVector.Builder(
          name = "add",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.35f, 12.64f)
            horizontalLineToRelative(-5f)
            quadToRelative(-0.28f, 0f, -0.46f, -0.19f)
            reflectiveQuadTo(5.7f, 11.99f)
            reflectiveQuadTo(5.89f, 11.52f)
            reflectiveQuadTo(6.35f, 11.34f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(-5f)
            quadToRelative(0f, -0.28f, 0.19f, -0.46f)
            reflectiveQuadTo(12f, 5.69f)
            reflectiveQuadToRelative(0.46f, 0.19f)
            reflectiveQuadToRelative(0.19f, 0.46f)
            verticalLineToRelative(5f)
            horizontalLineToRelative(5f)
            quadToRelative(0.28f, 0f, 0.46f, 0.19f)
            reflectiveQuadToRelative(0.19f, 0.47f)
            reflectiveQuadToRelative(-0.19f, 0.46f)
            reflectiveQuadToRelative(-0.46f, 0.18f)
            horizontalLineToRelative(-5f)
            verticalLineToRelative(5f)
            quadToRelative(0f, 0.28f, -0.19f, 0.46f)
            reflectiveQuadTo(12f, 18.29f)
            reflectiveQuadTo(11.54f, 18.1f)
            reflectiveQuadTo(11.35f, 17.64f)
            verticalLineToRelative(-5f)
            close()
          }
        }
        .build().also { _addW300 = it }
        IconWeight.W400 -> _addW400 ?: ImageVector.Builder(
          name = "add",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11f, 13f)
            horizontalLineTo(6f)
            quadTo(5.58f, 13f, 5.29f, 12.71f)
            quadTo(5f, 12.43f, 5f, 12f)
            reflectiveQuadTo(5.29f, 11.29f)
            reflectiveQuadTo(6f, 11f)
            horizontalLineToRelative(5f)
            verticalLineTo(6f)
            quadTo(11f, 5.57f, 11.29f, 5.29f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 6f)
            verticalLineToRelative(5f)
            horizontalLineToRelative(5f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(18f, 13f)
            horizontalLineTo(13f)
            verticalLineToRelative(5f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadTo(11.29f, 18.71f)
            quadTo(11f, 18.43f, 11f, 18f)
            verticalLineTo(13f)
            close()
          }
        }
        .build().also { _addW400 = it }
        IconWeight.W500 -> _addW500 ?: ImageVector.Builder(
          name = "add",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.86f, 13.14f)
            horizontalLineTo(5.93f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(4.8f, 12f)
            reflectiveQuadTo(5.13f, 11.19f)
            reflectiveQuadTo(5.93f, 10.86f)
            horizontalLineToRelative(4.93f)
            verticalLineTo(5.93f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(12f, 4.8f)
            reflectiveQuadToRelative(0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            verticalLineToRelative(4.93f)
            horizontalLineToRelative(4.93f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(19.2f, 12f)
            reflectiveQuadToRelative(-0.33f, 0.81f)
            reflectiveQuadToRelative(-0.81f, 0.33f)
            horizontalLineTo(13.14f)
            verticalLineToRelative(4.93f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(12f, 19.2f)
            reflectiveQuadTo(11.19f, 18.87f)
            reflectiveQuadTo(10.86f, 18.07f)
            verticalLineTo(13.14f)
            close()
          }
        }
        .build().also { _addW500 = it }
    }

private var _addW300: ImageVector? = null
private var _addW400: ImageVector? = null
private var _addW500: ImageVector? = null

public val FujiIcons.ArrowBack: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _arrowBackW300 ?: ImageVector.Builder(
          name = "arrow_back",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(7.39f, 12.7f)
            lineToRelative(5.11f, 5.11f)
            quadToRelative(0.21f, 0.22f, 0.21f, 0.5f)
            reflectiveQuadTo(12.5f, 18.8f)
            quadTo(12.28f, 19.02f, 12f, 19.02f)
            reflectiveQuadTo(11.51f, 18.8f)
            lineTo(5.31f, 12.6f)
            quadTo(5.17f, 12.47f, 5.11f, 12.32f)
            reflectiveQuadTo(5.05f, 12f)
            reflectiveQuadTo(5.11f, 11.68f)
            reflectiveQuadTo(5.31f, 11.4f)
            lineToRelative(6.2f, -6.2f)
            quadToRelative(0.2f, -0.2f, 0.49f, -0.21f)
            reflectiveQuadTo(12.5f, 5.2f)
            quadToRelative(0.22f, 0.22f, 0.22f, 0.5f)
            reflectiveQuadTo(12.5f, 6.2f)
            lineTo(7.39f, 11.3f)
            horizontalLineTo(18.6f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadTo(19.3f, 12f)
            reflectiveQuadToRelative(-0.21f, 0.49f)
            reflectiveQuadTo(18.6f, 12.7f)
            horizontalLineTo(7.39f)
            close()
          }
        }
        .build().also { _arrowBackW300 = it }
        IconWeight.W400 -> _arrowBackW400 ?: ImageVector.Builder(
          name = "arrow_back",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(7.83f, 13f)
            lineToRelative(4.9f, 4.9f)
            quadToRelative(0.3f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadTo(12.7f, 19.3f)
            quadTo(12.4f, 19.58f, 12f, 19.59f)
            reflectiveQuadTo(11.3f, 19.3f)
            lineTo(4.7f, 12.7f)
            quadTo(4.55f, 12.55f, 4.49f, 12.38f)
            reflectiveQuadTo(4.43f, 12f)
            reflectiveQuadTo(4.49f, 11.63f)
            reflectiveQuadTo(4.7f, 11.3f)
            lineTo(11.3f, 4.7f)
            quadTo(11.58f, 4.42f, 11.99f, 4.42f)
            reflectiveQuadTo(12.7f, 4.7f)
            quadTo(13f, 5f, 13f, 5.41f)
            reflectiveQuadTo(12.7f, 6.13f)
            lineTo(7.83f, 11f)
            horizontalLineTo(19f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(19f, 13f)
            horizontalLineTo(7.83f)
            close()
          }
        }
        .build().also { _arrowBackW400 = it }
        IconWeight.W500 -> _arrowBackW500 ?: ImageVector.Builder(
          name = "arrow_back",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.15f, 13.14f)
            lineToRelative(4.67f, 4.67f)
            quadToRelative(0.34f, 0.34f, 0.33f, 0.8f)
            reflectiveQuadToRelative(-0.35f, 0.8f)
            quadTo(12.46f, 19.72f, 12f, 19.73f)
            reflectiveQuadTo(11.2f, 19.4f)
            lineTo(4.6f, 12.8f)
            quadTo(4.43f, 12.63f, 4.35f, 12.43f)
            reflectiveQuadTo(4.28f, 12f)
            reflectiveQuadTo(4.35f, 11.57f)
            reflectiveQuadTo(4.6f, 11.2f)
            lineTo(11.2f, 4.59f)
            quadTo(11.53f, 4.27f, 11.99f, 4.27f)
            reflectiveQuadTo(12.8f, 4.59f)
            quadToRelative(0.34f, 0.34f, 0.34f, 0.81f)
            reflectiveQuadTo(12.8f, 6.21f)
            lineTo(8.15f, 10.86f)
            horizontalLineTo(19.07f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(20.2f, 12f)
            reflectiveQuadToRelative(-0.33f, 0.81f)
            reflectiveQuadToRelative(-0.81f, 0.33f)
            horizontalLineTo(8.15f)
            close()
          }
        }
        .build().also { _arrowBackW500 = it }
    }

private var _arrowBackW300: ImageVector? = null
private var _arrowBackW400: ImageVector? = null
private var _arrowBackW500: ImageVector? = null

public val FujiIcons.ArrowDownwardAlt: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _arrowDownwardAltW300 ?: ImageVector.Builder(
          name = "arrow_downward_alt",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.31f, 14.86f)
            verticalLineTo(6.07f)
            quadToRelative(0f, -0.29f, 0.21f, -0.49f)
            reflectiveQuadTo(12.01f, 5.37f)
            reflectiveQuadTo(12.5f, 5.58f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            verticalLineToRelative(8.78f)
            lineToRelative(3.36f, -3.36f)
            quadToRelative(0.2f, -0.2f, 0.48f, -0.2f)
            reflectiveQuadToRelative(0.49f, 0.21f)
            quadToRelative(0.2f, 0.2f, 0.2f, 0.49f)
            reflectiveQuadToRelative(-0.2f, 0.49f)
            lineTo(12.6f, 16.91f)
            quadTo(12.34f, 17.17f, 12f, 17.17f)
            reflectiveQuadTo(11.4f, 16.91f)
            lineTo(6.97f, 12.47f)
            quadToRelative(-0.2f, -0.2f, -0.2f, -0.48f)
            reflectiveQuadTo(6.97f, 11.5f)
            quadTo(7.19f, 11.29f, 7.46f, 11.29f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            lineToRelative(3.35f, 3.36f)
            close()
          }
        }
        .build().also { _arrowDownwardAltW300 = it }
        IconWeight.W400 -> _arrowDownwardAltW400 ?: ImageVector.Builder(
          name = "arrow_downward_alt",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11f, 14.2f)
            verticalLineTo(6f)
            quadTo(11f, 5.57f, 11.29f, 5.29f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 6f)
            verticalLineToRelative(8.2f)
            lineToRelative(2.9f, -2.9f)
            quadToRelative(0.28f, -0.28f, 0.7f, -0.28f)
            reflectiveQuadToRelative(0.7f, 0.28f)
            reflectiveQuadTo(17.58f, 12f)
            reflectiveQuadTo(17.3f, 12.7f)
            lineToRelative(-4.6f, 4.6f)
            quadTo(12.4f, 17.6f, 12f, 17.6f)
            reflectiveQuadTo(11.3f, 17.3f)
            lineTo(6.7f, 12.7f)
            quadTo(6.43f, 12.43f, 6.43f, 12f)
            reflectiveQuadTo(6.7f, 11.3f)
            reflectiveQuadTo(7.4f, 11.02f)
            reflectiveQuadTo(8.1f, 11.3f)
            lineTo(11f, 14.2f)
            close()
          }
        }
        .build().also { _arrowDownwardAltW400 = it }
        IconWeight.W500 -> _arrowDownwardAltW500 ?: ImageVector.Builder(
          name = "arrow_downward_alt",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.86f, 13.87f)
            verticalLineTo(6f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(12f, 4.86f)
            reflectiveQuadToRelative(0.81f, 0.33f)
            reflectiveQuadTo(13.14f, 6f)
            verticalLineToRelative(7.87f)
            lineTo(15.8f, 11.2f)
            quadToRelative(0.32f, -0.32f, 0.8f, -0.32f)
            reflectiveQuadToRelative(0.8f, 0.32f)
            reflectiveQuadTo(17.71f, 12f)
            reflectiveQuadTo(17.4f, 12.8f)
            lineTo(12.8f, 17.39f)
            quadTo(12.46f, 17.73f, 12f, 17.73f)
            reflectiveQuadTo(11.2f, 17.39f)
            lineTo(6.6f, 12.8f)
            quadTo(6.29f, 12.48f, 6.29f, 12f)
            reflectiveQuadTo(6.6f, 11.2f)
            reflectiveQuadTo(7.4f, 10.89f)
            reflectiveQuadTo(8.2f, 11.2f)
            lineToRelative(2.67f, 2.67f)
            close()
          }
        }
        .build().also { _arrowDownwardAltW500 = it }
    }

private var _arrowDownwardAltW300: ImageVector? = null
private var _arrowDownwardAltW400: ImageVector? = null
private var _arrowDownwardAltW500: ImageVector? = null

public val FujiIcons.ArrowForward: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _arrowForwardW300 ?: ImageVector.Builder(
          name = "arrow_forward",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(16.61f, 12.7f)
            horizontalLineTo(5.4f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.2f)
            reflectiveQuadTo(4.7f, 12f)
            reflectiveQuadTo(4.91f, 11.5f)
            reflectiveQuadTo(5.4f, 11.3f)
            horizontalLineToRelative(11.2f)
            lineTo(11.5f, 6.19f)
            quadTo(11.29f, 5.98f, 11.29f, 5.7f)
            reflectiveQuadTo(11.51f, 5.2f)
            quadTo(11.72f, 4.98f, 12f, 4.98f)
            reflectiveQuadTo(12.5f, 5.2f)
            lineToRelative(6.2f, 6.2f)
            quadToRelative(0.13f, 0.13f, 0.19f, 0.28f)
            reflectiveQuadTo(18.95f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.32f)
            reflectiveQuadTo(18.7f, 12.6f)
            lineToRelative(-6.2f, 6.2f)
            quadToRelative(-0.21f, 0.21f, -0.49f, 0.21f)
            reflectiveQuadTo(11.51f, 18.8f)
            reflectiveQuadTo(11.29f, 18.3f)
            reflectiveQuadToRelative(0.22f, -0.5f)
            lineToRelative(5.1f, -5.11f)
            close()
          }
        }
        .build().also { _arrowForwardW300 = it }
        IconWeight.W400 -> _arrowForwardW400 ?: ImageVector.Builder(
          name = "arrow_forward",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(16.18f, 13f)
            horizontalLineTo(5f)
            quadTo(4.58f, 13f, 4.29f, 12.71f)
            quadTo(4f, 12.43f, 4f, 12f)
            reflectiveQuadTo(4.29f, 11.29f)
            reflectiveQuadTo(5f, 11f)
            horizontalLineTo(16.18f)
            lineTo(11.28f, 6.1f)
            quadTo(10.98f, 5.8f, 10.99f, 5.4f)
            reflectiveQuadTo(11.3f, 4.7f)
            quadTo(11.6f, 4.42f, 12f, 4.41f)
            reflectiveQuadTo(12.7f, 4.7f)
            lineToRelative(6.6f, 6.6f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
            reflectiveQuadTo(19.58f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.38f)
            reflectiveQuadTo(19.3f, 12.7f)
            lineToRelative(-6.6f, 6.6f)
            quadToRelative(-0.28f, 0.27f, -0.69f, 0.27f)
            reflectiveQuadTo(11.3f, 19.3f)
            quadTo(11f, 19f, 11f, 18.59f)
            quadToRelative(0f, -0.41f, 0.3f, -0.71f)
            lineTo(16.18f, 13f)
            close()
          }
        }
        .build().also { _arrowForwardW400 = it }
        IconWeight.W500 -> _arrowForwardW500 ?: ImageVector.Builder(
          name = "arrow_forward",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(15.85f, 13.14f)
            horizontalLineTo(4.93f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(3.8f, 12f)
            reflectiveQuadTo(4.13f, 11.19f)
            reflectiveQuadTo(4.93f, 10.86f)
            horizontalLineTo(15.85f)
            lineTo(11.18f, 6.2f)
            quadTo(10.84f, 5.85f, 10.85f, 5.39f)
            reflectiveQuadTo(11.2f, 4.59f)
            quadTo(11.54f, 4.28f, 12f, 4.27f)
            reflectiveQuadTo(12.8f, 4.6f)
            lineToRelative(6.6f, 6.6f)
            quadToRelative(0.17f, 0.17f, 0.25f, 0.37f)
            reflectiveQuadTo(19.72f, 12f)
            reflectiveQuadToRelative(-0.08f, 0.43f)
            reflectiveQuadTo(19.4f, 12.8f)
            lineTo(12.8f, 19.41f)
            quadToRelative(-0.32f, 0.32f, -0.79f, 0.32f)
            reflectiveQuadTo(11.2f, 19.41f)
            quadTo(10.86f, 19.07f, 10.86f, 18.6f)
            reflectiveQuadTo(11.2f, 17.79f)
            lineToRelative(4.65f, -4.65f)
            close()
          }
        }
        .build().also { _arrowForwardW500 = it }
    }

private var _arrowForwardW300: ImageVector? = null
private var _arrowForwardW400: ImageVector? = null
private var _arrowForwardW500: ImageVector? = null

public val FujiIcons.ArrowUpwardAlt: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _arrowUpwardAltW300 ?: ImageVector.Builder(
          name = "arrow_upward_alt",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.29f, 8.03f)
            lineTo(7.94f, 11.38f)
            quadToRelative(-0.2f, 0.2f, -0.49f, 0.21f)
            reflectiveQuadTo(6.95f, 11.38f)
            quadTo(6.74f, 11.17f, 6.74f, 10.89f)
            reflectiveQuadTo(6.95f, 10.4f)
            lineTo(11.38f, 5.97f)
            quadToRelative(0.26f, -0.26f, 0.6f, -0.26f)
            reflectiveQuadToRelative(0.6f, 0.26f)
            lineToRelative(4.44f, 4.45f)
            quadToRelative(0.2f, 0.2f, 0.2f, 0.48f)
            reflectiveQuadToRelative(-0.2f, 0.49f)
            reflectiveQuadToRelative(-0.49f, 0.2f)
            reflectiveQuadToRelative(-0.49f, -0.2f)
            lineTo(12.69f, 8.03f)
            verticalLineToRelative(8.78f)
            quadToRelative(0f, 0.29f, -0.21f, 0.5f)
            reflectiveQuadToRelative(-0.49f, 0.21f)
            reflectiveQuadTo(11.5f, 17.31f)
            reflectiveQuadToRelative(-0.21f, -0.5f)
            verticalLineTo(8.03f)
            close()
          }
        }
        .build().also { _arrowUpwardAltW300 = it }
        IconWeight.W400 -> _arrowUpwardAltW400 ?: ImageVector.Builder(
          name = "arrow_upward_alt",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11f, 8.8f)
            lineTo(8.1f, 11.7f)
            quadTo(7.83f, 11.98f, 7.4f, 11.98f)
            reflectiveQuadTo(6.7f, 11.7f)
            reflectiveQuadTo(6.43f, 11f)
            reflectiveQuadTo(6.7f, 10.3f)
            lineTo(11.3f, 5.7f)
            quadTo(11.6f, 5.4f, 12f, 5.4f)
            reflectiveQuadToRelative(0.7f, 0.3f)
            lineToRelative(4.6f, 4.6f)
            quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
            reflectiveQuadTo(17.3f, 11.7f)
            reflectiveQuadToRelative(-0.7f, 0.28f)
            reflectiveQuadTo(15.9f, 11.7f)
            lineTo(13f, 8.8f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadTo(11.29f, 17.71f)
            quadTo(11f, 17.43f, 11f, 17f)
            verticalLineTo(8.8f)
            close()
          }
        }
        .build().also { _arrowUpwardAltW400 = it }
        IconWeight.W500 -> _arrowUpwardAltW500 ?: ImageVector.Builder(
          name = "arrow_upward_alt",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.86f, 9.18f)
            lineTo(8.2f, 11.85f)
            quadTo(7.88f, 12.17f, 7.4f, 12.17f)
            reflectiveQuadTo(6.6f, 11.85f)
            reflectiveQuadTo(6.29f, 11.05f)
            reflectiveQuadTo(6.6f, 10.26f)
            lineTo(11.2f, 5.66f)
            quadTo(11.54f, 5.32f, 12f, 5.32f)
            reflectiveQuadToRelative(0.8f, 0.34f)
            lineToRelative(4.59f, 4.59f)
            quadToRelative(0.32f, 0.32f, 0.32f, 0.8f)
            reflectiveQuadToRelative(-0.32f, 0.8f)
            reflectiveQuadToRelative(-0.8f, 0.32f)
            reflectiveQuadTo(15.8f, 11.85f)
            lineTo(13.14f, 9.18f)
            verticalLineToRelative(7.87f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(12f, 18.19f)
            reflectiveQuadTo(11.19f, 17.86f)
            reflectiveQuadTo(10.86f, 17.05f)
            verticalLineTo(9.18f)
            close()
          }
        }
        .build().also { _arrowUpwardAltW500 = it }
    }

private var _arrowUpwardAltW300: ImageVector? = null
private var _arrowUpwardAltW400: ImageVector? = null
private var _arrowUpwardAltW500: ImageVector? = null

public val FujiIcons.BookmarkStacks: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _bookmarkStacksW300 ?: ImageVector.Builder(
          name = "bookmark_stacks",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.73f, 3.73f)
            lineTo(12.71f, 7.45f)
            horizontalLineToRelative(7.48f)
            quadToRelative(0.41f, 0f, 0.63f, 0.26f)
            reflectiveQuadToRelative(0.23f, 0.59f)
            quadToRelative(0f, 0.22f, -0.12f, 0.42f)
            reflectiveQuadTo(20.58f, 9.07f)
            lineTo(12.8f, 13.19f)
            quadToRelative(-0.38f, 0.2f, -0.79f, 0.2f)
            reflectiveQuadToRelative(-0.79f, -0.2f)
            lineTo(3.14f, 8.91f)
            quadTo(2.9f, 8.79f, 2.8f, 8.59f)
            reflectiveQuadTo(2.69f, 8.16f)
            reflectiveQuadTo(2.8f, 7.73f)
            reflectiveQuadTo(3.14f, 7.4f)
            lineTo(11.47f, 2.97f)
            quadToRelative(0.22f, -0.11f, 0.44f, -0.1f)
            reflectiveQuadToRelative(0.4f, 0.13f)
            reflectiveQuadToRelative(0.3f, 0.3f)
            reflectiveQuadToRelative(0.12f, 0.43f)
            close()
            moveToRelative(-0.72f, 8.3f)
            lineTo(18.09f, 8.85f)
            horizontalLineTo(11.31f)
            verticalLineTo(4.66f)
            lineTo(4.64f, 8.16f)
            lineToRelative(7.38f, 3.87f)
            close()
            moveTo(11.31f, 8.85f)
            close()
            moveToRelative(0.7f, 7f)
            lineToRelative(7.93f, -4.18f)
            quadToRelative(0.07f, -0.04f, 0.39f, -0.05f)
            quadToRelative(0.27f, 0.02f, 0.45f, 0.21f)
            reflectiveQuadToRelative(0.18f, 0.47f)
            quadToRelative(0f, 0.19f, -0.09f, 0.35f)
            reflectiveQuadTo(20.6f, 12.91f)
            lineTo(12.8f, 17.02f)
            quadToRelative(-0.19f, 0.11f, -0.39f, 0.16f)
            reflectiveQuadToRelative(-0.4f, 0.05f)
            reflectiveQuadToRelative(-0.4f, -0.05f)
            reflectiveQuadTo(11.22f, 17.02f)
            lineTo(3.42f, 12.91f)
            quadTo(3.23f, 12.8f, 3.14f, 12.65f)
            reflectiveQuadTo(3.05f, 12.3f)
            quadToRelative(0f, -0.27f, 0.19f, -0.46f)
            reflectiveQuadTo(3.69f, 11.63f)
            quadToRelative(0.1f, -0.02f, 0.2f, -0f)
            reflectiveQuadToRelative(0.19f, 0.06f)
            lineToRelative(7.93f, 4.18f)
            close()
            moveToRelative(0f, 3.84f)
            lineToRelative(7.93f, -4.18f)
            quadToRelative(0.07f, -0.04f, 0.39f, -0.05f)
            quadToRelative(0.27f, 0.02f, 0.45f, 0.21f)
            reflectiveQuadToRelative(0.18f, 0.47f)
            quadToRelative(0f, 0.19f, -0.09f, 0.35f)
            reflectiveQuadTo(20.6f, 16.75f)
            lineTo(12.8f, 20.87f)
            quadToRelative(-0.19f, 0.11f, -0.39f, 0.16f)
            reflectiveQuadToRelative(-0.4f, 0.05f)
            reflectiveQuadToRelative(-0.4f, -0.05f)
            reflectiveQuadTo(11.22f, 20.87f)
            lineTo(3.42f, 16.75f)
            quadTo(3.23f, 16.65f, 3.14f, 16.49f)
            reflectiveQuadTo(3.05f, 16.15f)
            quadToRelative(0f, -0.27f, 0.19f, -0.46f)
            reflectiveQuadTo(3.69f, 15.47f)
            quadToRelative(0.1f, -0.02f, 0.2f, -0f)
            reflectiveQuadToRelative(0.19f, 0.06f)
            lineToRelative(7.93f, 4.18f)
            close()
          }
        }
        .build().also { _bookmarkStacksW300 = it }
        IconWeight.W400 -> _bookmarkStacksW400 ?: ImageVector.Builder(
          name = "bookmark_stacks",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(13f, 3.13f)
            verticalLineTo(7f)
            horizontalLineToRelative(7.9f)
            quadToRelative(0.47f, 0f, 0.75f, 0.31f)
            reflectiveQuadTo(21.93f, 8f)
            quadToRelative(0f, 0.25f, -0.13f, 0.49f)
            quadTo(21.68f, 8.73f, 21.4f, 8.88f)
            lineToRelative(-8.45f, 4.6f)
            quadTo(12.5f, 13.73f, 12f, 13.73f)
            reflectiveQuadTo(11.05f, 13.48f)
            lineTo(2.6f, 8.88f)
            quadTo(2.33f, 8.73f, 2.21f, 8.5f)
            quadTo(2.1f, 8.27f, 2.1f, 8f)
            quadTo(2.1f, 7.72f, 2.21f, 7.5f)
            quadTo(2.33f, 7.27f, 2.6f, 7.13f)
            lineTo(11.53f, 2.25f)
            quadTo(11.78f, 2.13f, 12.05f, 2.14f)
            reflectiveQuadToRelative(0.47f, 0.14f)
            reflectiveQuadToRelative(0.34f, 0.35f)
            reflectiveQuadTo(13f, 3.13f)
            close()
            moveToRelative(-1f, 8.6f)
            lineTo(17f, 9f)
            horizontalLineTo(11f)
            verticalLineTo(4.82f)
            lineTo(5.18f, 8f)
            lineTo(12f, 11.73f)
            close()
            moveTo(11f, 9f)
            close()
            moveToRelative(1f, 6.73f)
            lineToRelative(7.85f, -4.28f)
            quadToRelative(0.05f, -0.03f, 0.48f, -0.13f)
            quadToRelative(0.42f, 0f, 0.71f, 0.29f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            quadToRelative(0f, 0.28f, -0.13f, 0.5f)
            reflectiveQuadTo(20.8f, 13.2f)
            lineToRelative(-7.85f, 4.28f)
            quadToRelative(-0.22f, 0.13f, -0.46f, 0.19f)
            reflectiveQuadTo(12f, 17.73f)
            reflectiveQuadTo(11.51f, 17.66f)
            reflectiveQuadTo(11.05f, 17.48f)
            lineTo(3.2f, 13.2f)
            quadTo(2.93f, 13.05f, 2.8f, 12.83f)
            reflectiveQuadTo(2.68f, 12.33f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(3.68f, 11.33f)
            quadToRelative(0.13f, 0f, 0.24f, 0.04f)
            reflectiveQuadToRelative(0.24f, 0.09f)
            lineTo(12f, 15.73f)
            close()
            moveToRelative(0f, 4f)
            lineToRelative(7.85f, -4.28f)
            quadToRelative(0.05f, -0.03f, 0.48f, -0.13f)
            quadToRelative(0.42f, 0f, 0.71f, 0.29f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            quadToRelative(0f, 0.28f, -0.13f, 0.5f)
            reflectiveQuadTo(20.8f, 17.2f)
            lineToRelative(-7.85f, 4.28f)
            quadToRelative(-0.22f, 0.13f, -0.46f, 0.19f)
            reflectiveQuadTo(12f, 21.73f)
            reflectiveQuadTo(11.51f, 21.66f)
            reflectiveQuadTo(11.05f, 21.48f)
            lineTo(3.2f, 17.2f)
            quadTo(2.93f, 17.05f, 2.8f, 16.83f)
            reflectiveQuadTo(2.68f, 16.33f)
            quadToRelative(0f, -0.42f, 0.29f, -0.71f)
            quadTo(3.25f, 15.33f, 3.68f, 15.33f)
            quadToRelative(0.13f, 0f, 0.24f, 0.04f)
            reflectiveQuadToRelative(0.24f, 0.09f)
            lineTo(12f, 19.73f)
            close()
          }
        }
        .build().also { _bookmarkStacksW400 = it }
        IconWeight.W500 -> _bookmarkStacksW500 ?: ImageVector.Builder(
          name = "bookmark_stacks",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(13.14f, 3.08f)
            lineTo(13.13f, 6.77f)
            horizontalLineToRelative(7.61f)
            quadToRelative(0.53f, 0f, 0.85f, 0.36f)
            reflectiveQuadTo(21.9f, 7.91f)
            quadToRelative(0f, 0.29f, -0.15f, 0.56f)
            reflectiveQuadTo(21.3f, 8.9f)
            lineToRelative(-8.22f, 4.47f)
            quadTo(12.57f, 13.65f, 12f, 13.65f)
            reflectiveQuadTo(10.92f, 13.37f)
            lineTo(2.64f, 8.88f)
            quadTo(2.33f, 8.71f, 2.2f, 8.45f)
            reflectiveQuadTo(2.06f, 7.87f)
            reflectiveQuadTo(2.2f, 7.31f)
            reflectiveQuadTo(2.64f, 6.88f)
            lineToRelative(8.82f, -4.8f)
            quadTo(11.75f, 1.93f, 12.05f, 1.95f)
            reflectiveQuadToRelative(0.54f, 0.16f)
            reflectiveQuadToRelative(0.39f, 0.4f)
            reflectiveQuadToRelative(0.16f, 0.57f)
            close()
            moveTo(12f, 11.44f)
            lineTo(16.5f, 8.99f)
            horizontalLineTo(10.86f)
            verticalLineTo(4.94f)
            lineTo(5.46f, 7.88f)
            lineTo(12f, 11.44f)
            close()
            moveTo(10.86f, 8.99f)
            close()
            moveTo(12f, 15.56f)
            lineToRelative(7.81f, -4.25f)
            quadToRelative(0.07f, -0.04f, 0.53f, -0.14f)
            quadToRelative(0.47f, 0f, 0.79f, 0.32f)
            reflectiveQuadToRelative(0.32f, 0.79f)
            quadToRelative(0f, 0.3f, -0.14f, 0.55f)
            reflectiveQuadToRelative(-0.44f, 0.42f)
            lineToRelative(-7.79f, 4.24f)
            quadToRelative(-0.25f, 0.14f, -0.53f, 0.21f)
            reflectiveQuadTo(12f, 17.77f)
            reflectiveQuadTo(11.44f, 17.7f)
            reflectiveQuadTo(10.92f, 17.49f)
            lineTo(3.13f, 13.25f)
            quadTo(2.84f, 13.09f, 2.7f, 12.84f)
            reflectiveQuadTo(2.56f, 12.28f)
            quadToRelative(0f, -0.47f, 0.32f, -0.79f)
            reflectiveQuadTo(3.66f, 11.18f)
            quadToRelative(0.14f, 0f, 0.27f, 0.04f)
            reflectiveQuadToRelative(0.26f, 0.1f)
            lineTo(12f, 15.56f)
            close()
            moveToRelative(0f, 4.12f)
            lineToRelative(7.81f, -4.25f)
            quadToRelative(0.08f, -0.04f, 0.53f, -0.13f)
            quadToRelative(0.47f, 0f, 0.79f, 0.32f)
            reflectiveQuadToRelative(0.32f, 0.79f)
            quadToRelative(0f, 0.3f, -0.14f, 0.55f)
            reflectiveQuadToRelative(-0.44f, 0.42f)
            lineToRelative(-7.79f, 4.23f)
            quadToRelative(-0.25f, 0.14f, -0.53f, 0.21f)
            reflectiveQuadTo(12f, 21.89f)
            reflectiveQuadTo(11.44f, 21.82f)
            reflectiveQuadTo(10.92f, 21.61f)
            lineTo(3.13f, 17.38f)
            quadTo(2.84f, 17.22f, 2.7f, 16.96f)
            reflectiveQuadTo(2.56f, 16.41f)
            quadToRelative(0f, -0.47f, 0.32f, -0.79f)
            reflectiveQuadTo(3.66f, 15.3f)
            quadToRelative(0.14f, 0f, 0.26f, 0.04f)
            reflectiveQuadToRelative(0.26f, 0.1f)
            lineTo(12f, 19.68f)
            close()
          }
        }
        .build().also { _bookmarkStacksW500 = it }
    }

private var _bookmarkStacksW300: ImageVector? = null
private var _bookmarkStacksW400: ImageVector? = null
private var _bookmarkStacksW500: ImageVector? = null

public val FujiIcons.Cable: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _cableW300 ?: ImageVector.Builder(
          name = "cable",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.07f, 20.59f)
            quadToRelative(-0.31f, 0f, -0.51f, -0.2f)
            reflectiveQuadTo(4.36f, 19.9f)
            verticalLineTo(18.84f)
            horizontalLineTo(4.23f)
            quadTo(3.91f, 18.78f, 3.65f, 18.56f)
            reflectiveQuadTo(3.38f, 17.99f)
            verticalLineTo(15.17f)
            quadToRelative(0f, -0.3f, 0.2f, -0.49f)
            reflectiveQuadToRelative(0.49f, -0.2f)
            horizontalLineTo(5.36f)
            verticalLineTo(7.04f)
            quadToRelative(0f, -1.52f, 1.07f, -2.59f)
            reflectiveQuadTo(9.04f, 3.37f)
            quadToRelative(1.52f, 0f, 2.59f, 1.08f)
            reflectiveQuadTo(12.7f, 7.04f)
            verticalLineToRelative(9.88f)
            quadToRelative(0f, 0.95f, 0.66f, 1.61f)
            reflectiveQuadToRelative(1.61f, 0.66f)
            reflectiveQuadToRelative(1.61f, -0.66f)
            reflectiveQuadToRelative(0.66f, -1.61f)
            verticalLineTo(9.5f)
            horizontalLineTo(15.95f)
            quadToRelative(-0.3f, 0f, -0.49f, -0.2f)
            reflectiveQuadTo(15.26f, 8.81f)
            verticalLineTo(5.99f)
            quadToRelative(0f, -0.35f, 0.26f, -0.57f)
            reflectiveQuadTo(16.11f, 5.14f)
            horizontalLineToRelative(0.13f)
            verticalLineTo(4.08f)
            quadToRelative(0f, -0.3f, 0.2f, -0.49f)
            reflectiveQuadToRelative(0.49f, -0.2f)
            horizontalLineToRelative(2f)
            quadToRelative(0.3f, 0f, 0.51f, 0.2f)
            reflectiveQuadToRelative(0.2f, 0.49f)
            verticalLineTo(5.14f)
            horizontalLineToRelative(0.12f)
            quadToRelative(0.33f, 0.06f, 0.59f, 0.28f)
            reflectiveQuadToRelative(0.26f, 0.57f)
            verticalLineTo(8.81f)
            quadToRelative(0f, 0.3f, -0.2f, 0.49f)
            reflectiveQuadTo(19.93f, 9.5f)
            horizontalLineTo(18.64f)
            verticalLineToRelative(7.42f)
            quadToRelative(0f, 1.52f, -1.08f, 2.59f)
            reflectiveQuadToRelative(-2.6f, 1.08f)
            reflectiveQuadTo(12.37f, 19.51f)
            reflectiveQuadTo(11.3f, 16.92f)
            verticalLineTo(7.04f)
            quadToRelative(0f, -0.95f, -0.67f, -1.61f)
            reflectiveQuadTo(9.03f, 4.77f)
            quadToRelative(-0.95f, 0f, -1.61f, 0.66f)
            reflectiveQuadTo(6.76f, 7.04f)
            verticalLineToRelative(7.44f)
            horizontalLineTo(8.05f)
            quadToRelative(0.29f, 0f, 0.49f, 0.2f)
            reflectiveQuadToRelative(0.2f, 0.49f)
            verticalLineToRelative(2.81f)
            quadToRelative(0f, 0.36f, -0.26f, 0.58f)
            reflectiveQuadTo(7.89f, 18.84f)
            horizontalLineTo(7.76f)
            verticalLineTo(19.9f)
            quadToRelative(0f, 0.3f, -0.2f, 0.49f)
            reflectiveQuadToRelative(-0.51f, 0.2f)
            horizontalLineTo(5.07f)
            close()
          }
        }
        .build().also { _cableW300 = it }
        IconWeight.W400 -> _cableW400 ?: ImageVector.Builder(
          name = "cable",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5f, 21f)
            quadTo(4.58f, 21f, 4.29f, 20.71f)
            quadTo(4f, 20.43f, 4f, 20f)
            verticalLineTo(19f)
            quadTo(3.73f, 18.73f, 3.36f, 18.56f)
            reflectiveQuadTo(3f, 18f)
            verticalLineTo(15f)
            quadTo(3f, 14.58f, 3.29f, 14.29f)
            reflectiveQuadTo(4f, 14f)
            horizontalLineTo(5f)
            verticalLineTo(7f)
            quadTo(5f, 5.35f, 6.18f, 4.17f)
            reflectiveQuadTo(9f, 3f)
            reflectiveQuadToRelative(2.83f, 1.17f)
            reflectiveQuadTo(13f, 7f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.82f, 0.59f, 1.41f)
            reflectiveQuadTo(15f, 19f)
            reflectiveQuadToRelative(1.41f, -0.59f)
            reflectiveQuadTo(17f, 17f)
            verticalLineTo(10f)
            horizontalLineTo(16f)
            quadTo(15.58f, 10f, 15.29f, 9.71f)
            reflectiveQuadTo(15f, 9f)
            verticalLineTo(6f)
            quadTo(15f, 5.6f, 15.36f, 5.44f)
            quadTo(15.73f, 5.27f, 16f, 5f)
            verticalLineTo(4f)
            quadTo(16f, 3.57f, 16.29f, 3.29f)
            reflectiveQuadTo(17f, 3f)
            horizontalLineToRelative(2f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 4f)
            verticalLineTo(5f)
            quadToRelative(0.28f, 0.27f, 0.64f, 0.44f)
            reflectiveQuadTo(21f, 6f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 10f)
            horizontalLineTo(19f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 1.65f, -1.18f, 2.82f)
            reflectiveQuadTo(15f, 21f)
            reflectiveQuadTo(12.18f, 19.83f)
            reflectiveQuadTo(11f, 17f)
            verticalLineTo(7f)
            quadTo(11f, 6.18f, 10.41f, 5.59f)
            reflectiveQuadTo(9f, 5f)
            quadTo(8.18f, 5f, 7.59f, 5.59f)
            quadTo(7f, 6.18f, 7f, 7f)
            verticalLineToRelative(7f)
            horizontalLineTo(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(9f, 15f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.4f, -0.36f, 0.56f)
            reflectiveQuadTo(8f, 19f)
            verticalLineToRelative(1f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(7f, 21f)
            horizontalLineTo(5f)
            close()
          }
        }
        .build().also { _cableW400 = it }
        IconWeight.W500 -> _cableW500 ?: ImageVector.Builder(
          name = "cable",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5f, 21.09f)
            quadToRelative(-0.46f, 0f, -0.78f, -0.31f)
            reflectiveQuadTo(3.91f, 20f)
            verticalLineTo(19.09f)
            quadTo(3.6f, 18.88f, 3.25f, 18.69f)
            reflectiveQuadTo(2.91f, 18.09f)
            verticalLineTo(15f)
            quadToRelative(0f, -0.46f, 0.31f, -0.78f)
            reflectiveQuadTo(4f, 13.91f)
            horizontalLineTo(4.91f)
            verticalLineTo(7f)
            quadToRelative(0f, -1.69f, 1.2f, -2.89f)
            reflectiveQuadTo(9f, 2.91f)
            reflectiveQuadToRelative(2.89f, 1.2f)
            reflectiveQuadTo(13.09f, 7f)
            verticalLineTo(17f)
            quadToRelative(0f, 0.79f, 0.56f, 1.35f)
            reflectiveQuadTo(15f, 18.91f)
            reflectiveQuadToRelative(1.35f, -0.56f)
            reflectiveQuadTo(16.91f, 17f)
            verticalLineTo(10.09f)
            horizontalLineTo(16f)
            quadToRelative(-0.46f, 0f, -0.78f, -0.31f)
            reflectiveQuadTo(14.91f, 9f)
            verticalLineTo(5.91f)
            quadToRelative(0f, -0.41f, 0.34f, -0.6f)
            reflectiveQuadToRelative(0.66f, -0.4f)
            verticalLineTo(4f)
            quadToRelative(0f, -0.46f, 0.31f, -0.78f)
            reflectiveQuadTo(17f, 2.91f)
            horizontalLineToRelative(2f)
            quadToRelative(0.46f, 0f, 0.78f, 0.31f)
            reflectiveQuadTo(20.09f, 4f)
            verticalLineTo(4.91f)
            quadToRelative(0.31f, 0.21f, 0.66f, 0.4f)
            reflectiveQuadToRelative(0.34f, 0.6f)
            verticalLineTo(9f)
            quadToRelative(0f, 0.46f, -0.31f, 0.78f)
            reflectiveQuadTo(20f, 10.09f)
            horizontalLineTo(19.09f)
            verticalLineTo(17f)
            quadToRelative(0f, 1.69f, -1.2f, 2.89f)
            reflectiveQuadTo(15f, 21.09f)
            reflectiveQuadToRelative(-2.89f, -1.2f)
            reflectiveQuadTo(10.91f, 17f)
            verticalLineTo(7f)
            quadToRelative(0f, -0.79f, -0.56f, -1.35f)
            reflectiveQuadTo(9f, 5.09f)
            reflectiveQuadTo(7.65f, 5.65f)
            reflectiveQuadTo(7.09f, 7f)
            verticalLineToRelative(6.91f)
            horizontalLineTo(8f)
            quadToRelative(0.46f, 0f, 0.78f, 0.31f)
            reflectiveQuadTo(9.09f, 15f)
            verticalLineToRelative(3.09f)
            quadToRelative(0f, 0.41f, -0.34f, 0.6f)
            reflectiveQuadToRelative(-0.66f, 0.4f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.46f, -0.31f, 0.78f)
            reflectiveQuadTo(7f, 21.09f)
            horizontalLineTo(5f)
            close()
          }
        }
        .build().also { _cableW500 = it }
    }

private var _cableW300: ImageVector? = null
private var _cableW400: ImageVector? = null
private var _cableW500: ImageVector? = null

public val FujiIcons.CameraRoll: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _cameraRollW300 ?: ImageVector.Builder(
          name = "camera_roll",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.41f, 21.3f)
            quadTo(3.7f, 21.3f, 3.2f, 20.8f)
            reflectiveQuadTo(2.7f, 19.59f)
            verticalLineTo(5.39f)
            quadTo(2.7f, 4.68f, 3.2f, 4.18f)
            reflectiveQuadTo(4.41f, 3.68f)
            horizontalLineTo(5.66f)
            verticalLineTo(2.6f)
            quadToRelative(0f, -0.39f, 0.26f, -0.64f)
            reflectiveQuadTo(6.56f, 1.7f)
            horizontalLineTo(9.54f)
            quadToRelative(0.39f, 0f, 0.65f, 0.26f)
            reflectiveQuadTo(10.44f, 2.6f)
            verticalLineTo(3.68f)
            horizontalLineToRelative(1.25f)
            quadToRelative(0.71f, 0f, 1.21f, 0.5f)
            reflectiveQuadToRelative(0.5f, 1.21f)
            verticalLineTo(5.66f)
            horizontalLineToRelative(6.19f)
            quadToRelative(0.71f, 0f, 1.21f, 0.5f)
            reflectiveQuadToRelative(0.5f, 1.21f)
            verticalLineTo(17.61f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(13.38f)
            verticalLineToRelative(0.27f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.19f, 0.5f)
            horizontalLineTo(4.41f)
            close()
            moveToRelative(0f, -1.4f)
            horizontalLineToRelative(7.28f)
            quadToRelative(0.13f, 0f, 0.22f, -0.09f)
            reflectiveQuadTo(12f, 19.59f)
            verticalLineTo(17.92f)
            horizontalLineToRelative(7.59f)
            quadToRelative(0.13f, 0f, 0.22f, -0.09f)
            reflectiveQuadTo(19.9f, 17.61f)
            verticalLineTo(7.36f)
            quadToRelative(0f, -0.13f, -0.09f, -0.22f)
            reflectiveQuadTo(19.59f, 7.06f)
            horizontalLineTo(12f)
            verticalLineTo(5.39f)
            quadTo(12f, 5.25f, 11.91f, 5.17f)
            reflectiveQuadTo(11.69f, 5.08f)
            horizontalLineTo(4.41f)
            quadToRelative(-0.13f, 0f, -0.22f, 0.09f)
            reflectiveQuadTo(4.1f, 5.39f)
            verticalLineToRelative(14.2f)
            quadToRelative(0f, 0.13f, 0.09f, 0.22f)
            reflectiveQuadTo(4.41f, 19.9f)
            close()
            moveTo(10.73f, 9.61f)
            quadToRelative(0.25f, -0.25f, 0.25f, -0.6f)
            reflectiveQuadTo(10.73f, 8.41f)
            reflectiveQuadTo(10.13f, 8.16f)
            reflectiveQuadTo(9.52f, 8.41f)
            reflectiveQuadTo(9.27f, 9.01f)
            reflectiveQuadToRelative(0.25f, 0.6f)
            reflectiveQuadToRelative(0.6f, 0.25f)
            reflectiveQuadToRelative(0.6f, -0.25f)
            close()
            moveToRelative(3.9f, 0f)
            quadToRelative(0.25f, -0.25f, 0.25f, -0.6f)
            reflectiveQuadTo(14.63f, 8.41f)
            reflectiveQuadTo(14.03f, 8.16f)
            reflectiveQuadToRelative(-0.6f, 0.25f)
            reflectiveQuadToRelative(-0.25f, 0.6f)
            reflectiveQuadToRelative(0.25f, 0.6f)
            reflectiveQuadToRelative(0.6f, 0.25f)
            reflectiveQuadToRelative(0.6f, -0.25f)
            close()
            moveToRelative(3.92f, 0f)
            quadTo(18.8f, 9.36f, 18.8f, 9.01f)
            reflectiveQuadTo(18.55f, 8.41f)
            reflectiveQuadTo(17.95f, 8.16f)
            reflectiveQuadToRelative(-0.6f, 0.25f)
            reflectiveQuadToRelative(-0.25f, 0.6f)
            reflectiveQuadToRelative(0.25f, 0.6f)
            reflectiveQuadToRelative(0.6f, 0.25f)
            reflectiveQuadToRelative(0.6f, -0.25f)
            close()
            moveToRelative(-7.82f, 6.96f)
            quadToRelative(0.25f, -0.25f, 0.25f, -0.6f)
            reflectiveQuadToRelative(-0.25f, -0.6f)
            reflectiveQuadToRelative(-0.6f, -0.25f)
            reflectiveQuadToRelative(-0.6f, 0.25f)
            reflectiveQuadToRelative(-0.25f, 0.6f)
            reflectiveQuadToRelative(0.25f, 0.6f)
            reflectiveQuadToRelative(0.6f, 0.25f)
            reflectiveQuadToRelative(0.6f, -0.25f)
            close()
            moveToRelative(3.9f, 0f)
            quadToRelative(0.25f, -0.25f, 0.25f, -0.6f)
            reflectiveQuadToRelative(-0.25f, -0.6f)
            reflectiveQuadToRelative(-0.6f, -0.25f)
            reflectiveQuadToRelative(-0.6f, 0.25f)
            reflectiveQuadToRelative(-0.25f, 0.6f)
            reflectiveQuadToRelative(0.25f, 0.6f)
            reflectiveQuadToRelative(0.6f, 0.25f)
            reflectiveQuadToRelative(0.6f, -0.25f)
            close()
            moveToRelative(3.92f, 0f)
            quadToRelative(0.25f, -0.25f, 0.25f, -0.6f)
            reflectiveQuadToRelative(-0.25f, -0.6f)
            reflectiveQuadToRelative(-0.6f, -0.25f)
            reflectiveQuadToRelative(-0.6f, 0.25f)
            reflectiveQuadToRelative(-0.25f, 0.6f)
            reflectiveQuadToRelative(0.25f, 0.6f)
            reflectiveQuadToRelative(0.6f, 0.25f)
            reflectiveQuadToRelative(0.6f, -0.25f)
            close()
            moveTo(8.05f, 12.49f)
            close()
          }
        }
        .build().also { _cameraRollW300 = it }
        IconWeight.W400 -> _cameraRollW400 ?: ImageVector.Builder(
          name = "camera_roll",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4f, 22f)
            quadTo(3.18f, 22f, 2.59f, 21.41f)
            reflectiveQuadTo(2f, 20f)
            verticalLineTo(5f)
            quadTo(2f, 4.17f, 2.59f, 3.59f)
            reflectiveQuadTo(4f, 3f)
            horizontalLineTo(5f)
            verticalLineTo(2f)
            quadTo(5f, 1.57f, 5.29f, 1.29f)
            reflectiveQuadTo(6f, 1f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(11f, 2f)
            verticalLineTo(3f)
            horizontalLineToRelative(1f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(14f, 5f)
            horizontalLineToRelative(6f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 6.18f, 22f, 7f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(14f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(12f, 22f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 20f)
            horizontalLineToRelative(8f)
            verticalLineTo(18f)
            horizontalLineToRelative(8f)
            verticalLineTo(7f)
            horizontalLineTo(12f)
            verticalLineTo(5f)
            horizontalLineTo(4f)
            verticalLineTo(20f)
            close()
            moveTo(10.71f, 9.71f)
            quadTo(11f, 9.42f, 11f, 9f)
            quadTo(11f, 8.57f, 10.71f, 8.29f)
            reflectiveQuadTo(10f, 8f)
            quadTo(9.58f, 8f, 9.29f, 8.29f)
            reflectiveQuadTo(9f, 9f)
            quadTo(9f, 9.42f, 9.29f, 9.71f)
            quadTo(9.58f, 10f, 10f, 10f)
            reflectiveQuadTo(10.71f, 9.71f)
            close()
            moveToRelative(4f, 0f)
            quadTo(15f, 9.42f, 15f, 9f)
            quadTo(15f, 8.57f, 14.71f, 8.29f)
            reflectiveQuadTo(14f, 8f)
            reflectiveQuadTo(13.29f, 8.29f)
            reflectiveQuadTo(13f, 9f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(14f, 10f)
            reflectiveQuadTo(14.71f, 9.71f)
            close()
            moveToRelative(4f, 0f)
            quadTo(19f, 9.42f, 19f, 9f)
            quadTo(19f, 8.57f, 18.71f, 8.29f)
            reflectiveQuadTo(18f, 8f)
            reflectiveQuadTo(17.29f, 8.29f)
            reflectiveQuadTo(17f, 9f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(18f, 10f)
            reflectiveQuadTo(18.71f, 9.71f)
            close()
            moveToRelative(-8f, 7f)
            quadTo(11f, 16.43f, 11f, 16f)
            reflectiveQuadTo(10.71f, 15.29f)
            reflectiveQuadTo(10f, 15f)
            quadTo(9.58f, 15f, 9.29f, 15.29f)
            reflectiveQuadTo(9f, 16f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            quadTo(9.58f, 17f, 10f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(4f, 0f)
            quadTo(15f, 16.43f, 15f, 16f)
            reflectiveQuadTo(14.71f, 15.29f)
            reflectiveQuadTo(14f, 15f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(13f, 16f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(14f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(4f, 0f)
            quadTo(19f, 16.43f, 19f, 16f)
            reflectiveQuadTo(18.71f, 15.29f)
            reflectiveQuadTo(18f, 15f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(17f, 16f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(18f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveTo(8f, 12.5f)
            close()
          }
        }
        .build().also { _cameraRollW400 = it }
        IconWeight.W500 -> _cameraRollW500 ?: ImageVector.Builder(
          name = "camera_roll",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.07f, 22.2f)
            quadToRelative(-0.94f, 0f, -1.6f, -0.67f)
            reflectiveQuadTo(1.8f, 19.93f)
            verticalLineTo(5.07f)
            quadToRelative(0f, -0.94f, 0.67f, -1.6f)
            reflectiveQuadTo(4.07f, 2.8f)
            horizontalLineTo(5.01f)
            verticalLineToRelative(-1f)
            quadTo(5.01f, 1.37f, 5.3f, 1.08f)
            reflectiveQuadTo(6.02f, 0.8f)
            horizontalLineToRelative(4.04f)
            quadToRelative(0.43f, 0f, 0.72f, 0.29f)
            reflectiveQuadTo(11.07f, 1.8f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(0.95f)
            quadToRelative(0.91f, 0f, 1.55f, 0.64f)
            reflectiveQuadTo(14.2f, 4.97f)
            horizontalLineToRelative(5.72f)
            quadToRelative(0.94f, 0f, 1.61f, 0.67f)
            reflectiveQuadTo(22.2f, 7.25f)
            verticalLineToRelative(10.5f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadToRelative(-1.61f, 0.67f)
            horizontalLineTo(14.2f)
            quadToRelative(0f, 0.9f, -0.64f, 1.54f)
            reflectiveQuadTo(12.01f, 22.2f)
            horizontalLineTo(4.07f)
            close()
            moveTo(4.07f, 19.93f)
            horizontalLineToRelative(7.94f)
            verticalLineTo(17.75f)
            horizontalLineToRelative(7.92f)
            verticalLineTo(7.25f)
            horizontalLineTo(12.01f)
            verticalLineTo(5.07f)
            horizontalLineTo(4.07f)
            verticalLineTo(19.93f)
            close()
            moveToRelative(6.6f, -9.89f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.75f)
            reflectiveQuadTo(10.67f, 8.55f)
            reflectiveQuadTo(9.93f, 8.25f)
            reflectiveQuadTo(9.18f, 8.55f)
            reflectiveQuadTo(8.88f, 9.29f)
            reflectiveQuadToRelative(0.3f, 0.75f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadToRelative(0.75f, -0.3f)
            close()
            moveToRelative(3.98f, 0f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.75f)
            reflectiveQuadTo(14.65f, 8.55f)
            reflectiveQuadTo(13.9f, 8.25f)
            reflectiveQuadToRelative(-0.75f, 0.3f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadToRelative(0.3f, 0.75f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadToRelative(0.75f, -0.3f)
            close()
            moveToRelative(3.98f, 0f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.75f)
            reflectiveQuadTo(18.63f, 8.55f)
            reflectiveQuadTo(17.88f, 8.25f)
            reflectiveQuadToRelative(-0.75f, 0.3f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadToRelative(0.3f, 0.75f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadToRelative(0.75f, -0.3f)
            close()
            moveToRelative(-7.95f, 6.41f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.75f)
            reflectiveQuadToRelative(-0.3f, -0.75f)
            reflectiveQuadTo(9.93f, 14.65f)
            reflectiveQuadToRelative(-0.75f, 0.3f)
            reflectiveQuadTo(8.88f, 15.7f)
            reflectiveQuadToRelative(0.3f, 0.75f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadToRelative(0.75f, -0.3f)
            close()
            moveToRelative(3.98f, 0f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.75f)
            reflectiveQuadToRelative(-0.3f, -0.75f)
            reflectiveQuadTo(13.9f, 14.65f)
            reflectiveQuadToRelative(-0.75f, 0.3f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadToRelative(0.3f, 0.75f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadToRelative(0.75f, -0.3f)
            close()
            moveToRelative(3.98f, 0f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.75f)
            reflectiveQuadToRelative(-0.3f, -0.75f)
            reflectiveQuadToRelative(-0.75f, -0.3f)
            reflectiveQuadToRelative(-0.75f, 0.3f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadToRelative(0.3f, 0.75f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadToRelative(0.75f, -0.3f)
            close()
            moveTo(8.04f, 12.5f)
            close()
          }
        }
        .build().also { _cameraRollW500 = it }
    }

private var _cameraRollW300: ImageVector? = null
private var _cameraRollW400: ImageVector? = null
private var _cameraRollW500: ImageVector? = null

public val FujiIcons.Check: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _checkW300 ?: ImageVector.Builder(
          name = "check",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.57f, 15.54f)
            lineTo(18.13f, 6.98f)
            quadTo(18.34f, 6.77f, 18.62f, 6.76f)
            reflectiveQuadToRelative(0.5f, 0.22f)
            reflectiveQuadToRelative(0.22f, 0.5f)
            reflectiveQuadToRelative(-0.22f, 0.5f)
            lineToRelative(-8.95f, 8.95f)
            quadToRelative(-0.26f, 0.26f, -0.6f, 0.26f)
            reflectiveQuadTo(8.97f, 16.93f)
            lineTo(4.88f, 12.83f)
            quadTo(4.66f, 12.62f, 4.66f, 12.34f)
            reflectiveQuadToRelative(0.22f, -0.5f)
            reflectiveQuadToRelative(0.5f, -0.22f)
            reflectiveQuadToRelative(0.5f, 0.22f)
            lineToRelative(3.69f, 3.7f)
            close()
          }
        }
        .build().also { _checkW300 = it }
        IconWeight.W400 -> _checkW400 ?: ImageVector.Builder(
          name = "check",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.55f, 15.15f)
            lineTo(18.03f, 6.68f)
            quadToRelative(0.3f, -0.3f, 0.7f, -0.3f)
            reflectiveQuadToRelative(0.7f, 0.3f)
            quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
            reflectiveQuadTo(19.43f, 8.1f)
            lineToRelative(-9.18f, 9.2f)
            quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
            reflectiveQuadTo(8.85f, 17.3f)
            lineTo(4.55f, 13f)
            quadTo(4.25f, 12.7f, 4.26f, 12.29f)
            reflectiveQuadTo(4.58f, 11.58f)
            reflectiveQuadToRelative(0.71f, -0.3f)
            reflectiveQuadTo(6f, 11.58f)
            lineToRelative(3.55f, 3.58f)
            close()
          }
        }
        .build().also { _checkW400 = it }
        IconWeight.W500 -> _checkW500 ?: ImageVector.Builder(
          name = "check",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.55f, 14.95f)
            lineTo(17.92f, 6.58f)
            quadToRelative(0.34f, -0.34f, 0.8f, -0.34f)
            reflectiveQuadToRelative(0.8f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.81f)
            reflectiveQuadTo(19.52f, 8.2f)
            lineToRelative(-9.17f, 9.19f)
            quadToRelative(-0.34f, 0.34f, -0.8f, 0.34f)
            reflectiveQuadTo(8.75f, 17.39f)
            lineTo(4.46f, 13.1f)
            quadTo(4.12f, 12.76f, 4.13f, 12.29f)
            reflectiveQuadTo(4.48f, 11.48f)
            reflectiveQuadTo(5.29f, 11.14f)
            reflectiveQuadTo(6.1f, 11.48f)
            lineToRelative(3.45f, 3.47f)
            close()
          }
        }
        .build().also { _checkW500 = it }
    }

private var _checkW300: ImageVector? = null
private var _checkW400: ImageVector? = null
private var _checkW500: ImageVector? = null

public val FujiIcons.CloseSmall: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _closeSmallW300 ?: ImageVector.Builder(
          name = "close_small",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 12.99f)
            lineTo(8.75f, 16.25f)
            quadToRelative(-0.21f, 0.21f, -0.5f, 0.21f)
            reflectiveQuadTo(7.76f, 16.24f)
            reflectiveQuadTo(7.55f, 15.74f)
            reflectiveQuadToRelative(0.21f, -0.5f)
            lineTo(11.01f, 12f)
            lineTo(7.75f, 8.77f)
            quadTo(7.54f, 8.55f, 7.54f, 8.27f)
            reflectiveQuadTo(7.76f, 7.76f)
            reflectiveQuadTo(8.26f, 7.55f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            lineTo(12f, 11.02f)
            lineTo(15.23f, 7.76f)
            quadTo(15.45f, 7.55f, 15.73f, 7.55f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            reflectiveQuadToRelative(0.22f, 0.51f)
            reflectiveQuadToRelative(-0.22f, 0.5f)
            lineTo(12.98f, 12f)
            lineToRelative(3.26f, 3.25f)
            quadToRelative(0.21f, 0.21f, 0.21f, 0.5f)
            reflectiveQuadToRelative(-0.21f, 0.5f)
            quadToRelative(-0.22f, 0.22f, -0.51f, 0.22f)
            reflectiveQuadToRelative(-0.5f, -0.22f)
            lineTo(12f, 12.99f)
            close()
          }
        }
        .build().also { _closeSmallW300 = it }
        IconWeight.W400 -> _closeSmallW400 ?: ImageVector.Builder(
          name = "close_small",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 13.4f)
            lineTo(9.1f, 16.3f)
            quadTo(8.83f, 16.58f, 8.4f, 16.58f)
            reflectiveQuadTo(7.7f, 16.3f)
            quadTo(7.43f, 16.02f, 7.43f, 15.6f)
            reflectiveQuadTo(7.7f, 14.9f)
            lineTo(10.6f, 12f)
            lineTo(7.7f, 9.13f)
            quadTo(7.43f, 8.85f, 7.43f, 8.42f)
            reflectiveQuadTo(7.7f, 7.72f)
            reflectiveQuadTo(8.4f, 7.45f)
            quadToRelative(0.43f, 0f, 0.7f, 0.27f)
            lineToRelative(2.9f, 2.9f)
            lineToRelative(2.88f, -2.9f)
            quadToRelative(0.28f, -0.27f, 0.7f, -0.27f)
            reflectiveQuadToRelative(0.7f, 0.27f)
            quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
            reflectiveQuadToRelative(-0.3f, 0.69f)
            lineTo(13.38f, 12f)
            lineToRelative(2.9f, 2.9f)
            quadToRelative(0.27f, 0.27f, 0.27f, 0.7f)
            reflectiveQuadToRelative(-0.27f, 0.7f)
            quadToRelative(-0.3f, 0.3f, -0.71f, 0.3f)
            reflectiveQuadTo(14.88f, 16.3f)
            lineTo(12f, 13.4f)
            close()
          }
        }
        .build().also { _closeSmallW400 = it }
        IconWeight.W500 -> _closeSmallW500 ?: ImageVector.Builder(
          name = "close_small",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 13.6f)
            lineTo(9.36f, 16.23f)
            quadTo(9.05f, 16.55f, 8.57f, 16.55f)
            reflectiveQuadTo(7.77f, 16.23f)
            reflectiveQuadTo(7.45f, 15.43f)
            reflectiveQuadToRelative(0.32f, -0.8f)
            lineTo(10.4f, 12f)
            lineTo(7.77f, 9.39f)
            quadTo(7.45f, 9.07f, 7.45f, 8.6f)
            reflectiveQuadTo(7.77f, 7.8f)
            quadTo(8.09f, 7.48f, 8.57f, 7.48f)
            reflectiveQuadTo(9.36f, 7.8f)
            lineTo(12f, 10.43f)
            lineTo(14.61f, 7.8f)
            quadToRelative(0.32f, -0.32f, 0.8f, -0.32f)
            reflectiveQuadTo(16.2f, 7.8f)
            quadToRelative(0.34f, 0.34f, 0.34f, 0.81f)
            reflectiveQuadTo(16.2f, 9.39f)
            lineTo(13.57f, 12f)
            lineToRelative(2.63f, 2.64f)
            quadToRelative(0.32f, 0.32f, 0.32f, 0.8f)
            reflectiveQuadToRelative(-0.32f, 0.8f)
            quadToRelative(-0.34f, 0.34f, -0.81f, 0.34f)
            reflectiveQuadTo(14.61f, 16.23f)
            lineTo(12f, 13.6f)
            close()
          }
        }
        .build().also { _closeSmallW500 = it }
    }

private var _closeSmallW300: ImageVector? = null
private var _closeSmallW400: ImageVector? = null
private var _closeSmallW500: ImageVector? = null

public val FujiIcons.Colors: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _colorsW300 ?: ImageVector.Builder(
          name = "colors",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.63f, 19.82f)
            lineTo(3.18f, 14.38f)
            quadTo(2.93f, 14.13f, 2.81f, 13.83f)
            reflectiveQuadTo(2.7f, 13.21f)
            quadTo(2.7f, 12.9f, 2.82f, 12.6f)
            reflectiveQuadTo(3.18f, 12.07f)
            lineTo(8.71f, 6.57f)
            lineTo(6.46f, 4.37f)
            quadTo(6.24f, 4.14f, 6.23f, 3.81f)
            reflectiveQuadTo(6.46f, 3.25f)
            quadTo(6.69f, 3.01f, 7.02f, 3.01f)
            reflectiveQuadTo(7.58f, 3.25f)
            lineToRelative(8.78f, 8.82f)
            quadToRelative(0.25f, 0.25f, 0.37f, 0.54f)
            reflectiveQuadToRelative(0.12f, 0.6f)
            quadToRelative(0f, 0.32f, -0.12f, 0.62f)
            reflectiveQuadToRelative(-0.37f, 0.54f)
            lineToRelative(-5.42f, 5.44f)
            quadToRelative(-0.25f, 0.25f, -0.54f, 0.37f)
            reflectiveQuadTo(9.8f, 20.31f)
            quadTo(9.47f, 20.32f, 9.17f, 20.2f)
            reflectiveQuadTo(8.63f, 19.82f)
            close()
            moveTo(9.78f, 7.64f)
            lineTo(4.4f, 13.02f)
            quadToRelative(-0.06f, 0.04f, -0.08f, 0.1f)
            reflectiveQuadTo(4.3f, 13.25f)
            horizontalLineTo(15.25f)
            quadToRelative(0f, -0.08f, -0.02f, -0.14f)
            reflectiveQuadToRelative(-0.08f, -0.1f)
            lineTo(9.78f, 7.64f)
            close()
            moveTo(19.42f, 20.3f)
            quadToRelative(-0.77f, 0f, -1.3f, -0.55f)
            reflectiveQuadTo(17.6f, 18.41f)
            quadToRelative(0f, -0.6f, 0.27f, -1.14f)
            reflectiveQuadToRelative(0.65f, -1.02f)
            lineToRelative(0.41f, -0.52f)
            quadToRelative(0.19f, -0.24f, 0.49f, -0.24f)
            reflectiveQuadToRelative(0.5f, 0.23f)
            lineToRelative(0.46f, 0.54f)
            quadToRelative(0.39f, 0.48f, 0.66f, 1.02f)
            reflectiveQuadToRelative(0.27f, 1.14f)
            quadToRelative(0f, 0.8f, -0.55f, 1.34f)
            reflectiveQuadTo(19.42f, 20.3f)
            close()
          }
        }
        .build().also { _colorsW300 = it }
        IconWeight.W400 -> _colorsW400 ?: ImageVector.Builder(
          name = "colors",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.65f, 20.5f)
            lineTo(2.5f, 14.35f)
            quadTo(2.25f, 14.1f, 2.13f, 13.8f)
            reflectiveQuadTo(2f, 13.18f)
            reflectiveQuadTo(2.13f, 12.55f)
            reflectiveQuadTo(2.5f, 12f)
            lineTo(8.25f, 6.27f)
            lineTo(6.38f, 4.4f)
            quadTo(6.05f, 4.07f, 6.04f, 3.63f)
            quadTo(6.03f, 3.17f, 6.35f, 2.82f)
            quadTo(6.68f, 2.47f, 7.15f, 2.47f)
            reflectiveQuadTo(7.98f, 2.82f)
            lineTo(17.15f, 12f)
            quadToRelative(0.25f, 0.25f, 0.36f, 0.55f)
            quadToRelative(0.11f, 0.3f, 0.11f, 0.63f)
            reflectiveQuadTo(17.51f, 13.8f)
            reflectiveQuadToRelative(-0.36f, 0.55f)
            lineTo(11f, 20.5f)
            quadToRelative(-0.25f, 0.25f, -0.55f, 0.38f)
            reflectiveQuadTo(9.83f, 21f)
            reflectiveQuadTo(9.2f, 20.88f)
            reflectiveQuadTo(8.65f, 20.5f)
            close()
            moveTo(9.83f, 7.85f)
            lineTo(4.48f, 13.2f)
            horizontalLineToRelative(10.7f)
            lineTo(9.83f, 7.85f)
            close()
            moveTo(19.8f, 21f)
            quadToRelative(-0.9f, 0f, -1.52f, -0.64f)
            reflectiveQuadTo(17.65f, 18.8f)
            quadToRelative(0f, -0.68f, 0.34f, -1.28f)
            quadToRelative(0.34f, -0.6f, 0.76f, -1.17f)
            lineToRelative(0.48f, -0.6f)
            quadToRelative(0.23f, -0.28f, 0.59f, -0.29f)
            quadToRelative(0.36f, -0.01f, 0.59f, 0.26f)
            lineToRelative(0.5f, 0.63f)
            quadToRelative(0.4f, 0.57f, 0.75f, 1.17f)
            quadTo(22f, 18.13f, 22f, 18.8f)
            quadToRelative(0f, 0.93f, -0.65f, 1.56f)
            reflectiveQuadTo(19.8f, 21f)
            close()
          }
        }
        .build().also { _colorsW400 = it }
        IconWeight.W500 -> _colorsW500 ?: ImageVector.Builder(
          name = "colors",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.29f, 20.6f)
            lineTo(2.4f, 14.72f)
            quadTo(2.09f, 14.42f, 1.95f, 14.05f)
            reflectiveQuadTo(1.8f, 13.29f)
            quadToRelative(0f, -0.39f, 0.15f, -0.76f)
            reflectiveQuadTo(2.4f, 11.86f)
            lineTo(7.97f, 6.32f)
            lineTo(6.09f, 4.44f)
            quadTo(5.73f, 4.08f, 5.72f, 3.58f)
            reflectiveQuadTo(6.07f, 2.7f)
            quadTo(6.44f, 2.31f, 6.96f, 2.32f)
            reflectiveQuadTo(7.86f, 2.7f)
            lineToRelative(9.16f, 9.16f)
            quadToRelative(0.3f, 0.3f, 0.44f, 0.67f)
            reflectiveQuadToRelative(0.14f, 0.76f)
            quadToRelative(0f, 0.4f, -0.14f, 0.76f)
            reflectiveQuadToRelative(-0.44f, 0.67f)
            lineTo(11.14f, 20.6f)
            quadToRelative(-0.3f, 0.3f, -0.67f, 0.45f)
            reflectiveQuadTo(9.71f, 21.2f)
            reflectiveQuadTo(8.95f, 21.05f)
            reflectiveQuadTo(8.29f, 20.6f)
            close()
            moveTo(9.71f, 8.07f)
            lineTo(4.53f, 13.24f)
            horizontalLineTo(14.88f)
            lineTo(9.71f, 8.07f)
            close()
            moveTo(19.94f, 21.2f)
            quadTo(19f, 21.2f, 18.36f, 20.54f)
            reflectiveQuadTo(17.72f, 18.91f)
            quadToRelative(0f, -0.67f, 0.31f, -1.27f)
            reflectiveQuadToRelative(0.74f, -1.15f)
            lineToRelative(0.52f, -0.64f)
            quadToRelative(0.25f, -0.3f, 0.66f, -0.31f)
            reflectiveQuadToRelative(0.66f, 0.3f)
            lineToRelative(0.54f, 0.65f)
            quadToRelative(0.41f, 0.56f, 0.74f, 1.15f)
            reflectiveQuadToRelative(0.33f, 1.27f)
            quadToRelative(0f, 0.96f, -0.66f, 1.63f)
            reflectiveQuadToRelative(-1.6f, 0.67f)
            close()
          }
        }
        .build().also { _colorsW500 = it }
    }

private var _colorsW300: ImageVector? = null
private var _colorsW400: ImageVector? = null
private var _colorsW500: ImageVector? = null

public val FujiIcons.ContentCopy: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _contentCopyW300 ?: ImageVector.Builder(
          name = "content_copy",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.96f, 17.5f)
            quadTo(8.25f, 17.5f, 7.75f, 17f)
            reflectiveQuadTo(7.25f, 15.79f)
            verticalLineTo(4.41f)
            quadTo(7.25f, 3.7f, 7.75f, 3.2f)
            reflectiveQuadTo(8.96f, 2.7f)
            horizontalLineToRelative(8.38f)
            quadToRelative(0.71f, 0f, 1.21f, 0.5f)
            reflectiveQuadToRelative(0.5f, 1.21f)
            verticalLineTo(15.79f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(8.96f)
            close()
            moveToRelative(0f, -1.4f)
            horizontalLineToRelative(8.38f)
            quadToRelative(0.12f, 0f, 0.21f, -0.1f)
            reflectiveQuadToRelative(0.1f, -0.21f)
            verticalLineTo(4.41f)
            quadToRelative(0f, -0.12f, -0.1f, -0.21f)
            reflectiveQuadTo(17.34f, 4.1f)
            horizontalLineTo(8.96f)
            quadTo(8.84f, 4.1f, 8.75f, 4.2f)
            reflectiveQuadTo(8.65f, 4.41f)
            verticalLineTo(15.79f)
            quadToRelative(0f, 0.12f, 0.1f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.1f)
            close()
            moveToRelative(-3.3f, 4.7f)
            quadToRelative(-0.71f, 0f, -1.21f, -0.5f)
            reflectiveQuadTo(3.95f, 19.09f)
            verticalLineTo(7f)
            quadToRelative(0f, -0.29f, 0.21f, -0.49f)
            reflectiveQuadTo(4.65f, 6.31f)
            reflectiveQuadTo(5.15f, 6.51f)
            reflectiveQuadTo(5.35f, 7f)
            verticalLineTo(19.09f)
            quadToRelative(0f, 0.12f, 0.1f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.1f)
            horizontalLineToRelative(9.09f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            quadToRelative(0f, 0.29f, -0.21f, 0.5f)
            reflectiveQuadTo(14.75f, 20.8f)
            horizontalLineTo(5.66f)
            close()
            moveTo(8.65f, 16.1f)
            quadToRelative(0f, 0f, 0f, -0.1f)
            reflectiveQuadToRelative(0f, -0.21f)
            verticalLineTo(4.41f)
            quadToRelative(0f, -0.12f, 0f, -0.21f)
            reflectiveQuadToRelative(0f, -0.1f)
            quadToRelative(0f, 0f, 0f, 0.1f)
            reflectiveQuadToRelative(0f, 0.21f)
            verticalLineTo(15.79f)
            quadToRelative(0f, 0.12f, 0f, 0.21f)
            reflectiveQuadToRelative(0f, 0.1f)
            close()
          }
        }
        .build().also { _contentCopyW300 = it }
        IconWeight.W400 -> _contentCopyW400 ?: ImageVector.Builder(
          name = "content_copy",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9f, 18f)
            quadTo(8.18f, 18f, 7.59f, 17.41f)
            reflectiveQuadTo(7f, 16f)
            verticalLineTo(4f)
            quadTo(7f, 3.17f, 7.59f, 2.59f)
            reflectiveQuadTo(9f, 2f)
            horizontalLineToRelative(9f)
            quadToRelative(0.82f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(20f, 4f)
            verticalLineTo(16f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 18f)
            horizontalLineTo(9f)
            close()
            moveTo(9f, 16f)
            horizontalLineToRelative(9f)
            verticalLineTo(4f)
            horizontalLineTo(9f)
            verticalLineTo(16f)
            close()
            moveTo(5f, 22f)
            quadTo(4.18f, 22f, 3.59f, 21.41f)
            reflectiveQuadTo(3f, 20f)
            verticalLineTo(7f)
            quadTo(3f, 6.57f, 3.29f, 6.29f)
            reflectiveQuadTo(4f, 6f)
            reflectiveQuadTo(4.71f, 6.29f)
            reflectiveQuadTo(5f, 7f)
            verticalLineTo(20f)
            horizontalLineTo(15f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(16f, 21f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(15f, 22f)
            horizontalLineTo(5f)
            close()
            moveTo(9f, 16f)
            verticalLineTo(4f)
            verticalLineTo(16f)
            close()
          }
        }
        .build().also { _contentCopyW400 = it }
        IconWeight.W500 -> _contentCopyW500 ?: ImageVector.Builder(
          name = "content_copy",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.21f, 18.07f)
            quadTo(8.26f, 18.07f, 7.6f, 17.4f)
            reflectiveQuadTo(6.93f, 15.79f)
            verticalLineTo(3.93f)
            quadTo(6.93f, 2.99f, 7.6f, 2.32f)
            reflectiveQuadTo(9.21f, 1.66f)
            horizontalLineToRelative(8.86f)
            quadToRelative(0.94f, 0f, 1.61f, 0.67f)
            reflectiveQuadToRelative(0.67f, 1.61f)
            verticalLineTo(15.79f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadToRelative(-1.61f, 0.67f)
            horizontalLineTo(9.21f)
            close()
            moveToRelative(0f, -2.28f)
            horizontalLineToRelative(8.86f)
            verticalLineTo(3.93f)
            horizontalLineTo(9.21f)
            verticalLineTo(15.79f)
            close()
            moveTo(4.93f, 22.34f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(2.66f, 20.07f)
            verticalLineTo(7.07f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(3.8f, 5.93f)
            reflectiveQuadTo(4.6f, 6.26f)
            reflectiveQuadTo(4.93f, 7.07f)
            verticalLineTo(20.07f)
            horizontalLineToRelative(9.99f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            reflectiveQuadToRelative(-0.33f, 0.81f)
            reflectiveQuadToRelative(-0.81f, 0.33f)
            horizontalLineTo(4.93f)
            close()
            moveTo(9.21f, 15.79f)
            verticalLineTo(3.93f)
            verticalLineTo(15.79f)
            close()
          }
        }
        .build().also { _contentCopyW500 = it }
    }

private var _contentCopyW300: ImageVector? = null
private var _contentCopyW400: ImageVector? = null
private var _contentCopyW500: ImageVector? = null

public val FujiIcons.ContentPaste: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _contentPasteW300 ?: ImageVector.Builder(
          name = "content_paste",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.39f, 20.32f)
            quadToRelative(-0.71f, 0f, -1.21f, -0.5f)
            reflectiveQuadTo(3.68f, 18.61f)
            verticalLineTo(5.39f)
            quadToRelative(0f, -0.71f, 0.5f, -1.21f)
            reflectiveQuadTo(5.39f, 3.68f)
            horizontalLineTo(9.83f)
            quadToRelative(0.19f, -0.73f, 0.79f, -1.2f)
            reflectiveQuadTo(12f, 2.01f)
            quadToRelative(0.78f, 0f, 1.38f, 0.47f)
            reflectiveQuadToRelative(0.79f, 1.2f)
            horizontalLineToRelative(4.44f)
            quadToRelative(0.71f, 0f, 1.21f, 0.5f)
            reflectiveQuadToRelative(0.5f, 1.21f)
            verticalLineTo(18.61f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(5.39f)
            close()
            moveToRelative(0f, -1.4f)
            horizontalLineTo(18.61f)
            quadToRelative(0.12f, 0f, 0.21f, -0.1f)
            reflectiveQuadToRelative(0.1f, -0.21f)
            verticalLineTo(5.39f)
            quadToRelative(0f, -0.12f, -0.1f, -0.21f)
            reflectiveQuadTo(18.61f, 5.08f)
            horizontalLineTo(16.52f)
            verticalLineTo(6.69f)
            quadToRelative(0f, 0.36f, -0.25f, 0.6f)
            reflectiveQuadToRelative(-0.6f, 0.25f)
            horizontalLineTo(8.34f)
            quadTo(7.98f, 7.54f, 7.73f, 7.3f)
            reflectiveQuadTo(7.48f, 6.69f)
            verticalLineTo(5.08f)
            horizontalLineTo(5.39f)
            quadToRelative(-0.12f, 0f, -0.21f, 0.1f)
            reflectiveQuadTo(5.08f, 5.39f)
            verticalLineTo(18.61f)
            quadToRelative(0f, 0.12f, 0.1f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.1f)
            close()
            moveTo(12.61f, 4.87f)
            quadToRelative(0.25f, -0.25f, 0.25f, -0.6f)
            reflectiveQuadTo(12.61f, 3.66f)
            reflectiveQuadTo(12f, 3.41f)
            reflectiveQuadTo(11.4f, 3.66f)
            reflectiveQuadToRelative(-0.25f, 0.6f)
            reflectiveQuadToRelative(0.25f, 0.6f)
            reflectiveQuadTo(12f, 5.12f)
            reflectiveQuadTo(12.61f, 4.87f)
            close()
          }
        }
        .build().also { _contentPasteW300 = it }
        IconWeight.W400 -> _contentPasteW400 ?: ImageVector.Builder(
          name = "content_paste",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(9.18f)
            quadTo(9.45f, 2.13f, 10.25f, 1.56f)
            reflectiveQuadTo(12f, 1f)
            quadToRelative(1f, 0f, 1.79f, 0.56f)
            reflectiveQuadTo(14.85f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 19f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            horizontalLineTo(17f)
            verticalLineTo(7f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(16f, 8f)
            horizontalLineTo(8f)
            quadTo(7.58f, 8f, 7.29f, 7.71f)
            quadTo(7f, 7.43f, 7f, 7f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            close()
            moveTo(12.71f, 4.71f)
            quadTo(13f, 4.42f, 13f, 4f)
            quadTo(13f, 3.57f, 12.71f, 3.29f)
            reflectiveQuadTo(12f, 3f)
            reflectiveQuadTo(11.29f, 3.29f)
            reflectiveQuadTo(11f, 4f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadTo(12.71f, 4.71f)
            close()
          }
        }
        .build().also { _contentPasteW400 = it }
        IconWeight.W500 -> _contentPasteW500 ?: ImageVector.Builder(
          name = "content_paste",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.07f, 21.2f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(2.8f, 18.93f)
            verticalLineTo(5.07f)
            quadTo(2.8f, 4.13f, 3.47f, 3.47f)
            reflectiveQuadTo(5.07f, 2.8f)
            horizontalLineTo(9.02f)
            quadTo(9.33f, 1.9f, 10.17f, 1.34f)
            reflectiveQuadTo(12f, 0.77f)
            quadToRelative(1.03f, 0f, 1.86f, 0.57f)
            reflectiveQuadTo(15.01f, 2.8f)
            horizontalLineToRelative(3.92f)
            quadToRelative(0.94f, 0f, 1.61f, 0.67f)
            reflectiveQuadTo(21.2f, 5.07f)
            verticalLineTo(18.93f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(18.93f, 21.2f)
            horizontalLineTo(5.07f)
            close()
            moveToRelative(0f, -2.28f)
            horizontalLineTo(18.93f)
            verticalLineTo(5.07f)
            horizontalLineTo(16.97f)
            verticalLineTo(7.03f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(15.83f, 8.17f)
            horizontalLineTo(8.17f)
            quadTo(7.69f, 8.17f, 7.36f, 7.84f)
            reflectiveQuadTo(7.03f, 7.03f)
            verticalLineTo(5.07f)
            horizontalLineTo(5.07f)
            verticalLineTo(18.93f)
            close()
            moveTo(12.71f, 4.69f)
            quadTo(13f, 4.41f, 13f, 3.98f)
            reflectiveQuadTo(12.71f, 3.27f)
            reflectiveQuadTo(12f, 2.98f)
            reflectiveQuadTo(11.29f, 3.27f)
            reflectiveQuadTo(11f, 3.98f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(12f, 4.98f)
            reflectiveQuadTo(12.71f, 4.69f)
            close()
          }
        }
        .build().also { _contentPasteW500 = it }
    }

private var _contentPasteW300: ImageVector? = null
private var _contentPasteW400: ImageVector? = null
private var _contentPasteW500: ImageVector? = null

public val FujiIcons.Contrast: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _contrastW300 ?: ImageVector.Builder(
          name = "contrast",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.38f, 20.57f)
            quadTo(6.68f, 19.83f, 5.42f, 18.58f)
            reflectiveQuadTo(3.44f, 15.63f)
            reflectiveQuadTo(2.7f, 12f)
            reflectiveQuadTo(3.43f, 8.38f)
            reflectiveQuadTo(5.42f, 5.42f)
            reflectiveQuadTo(8.37f, 3.44f)
            reflectiveQuadTo(12f, 2.7f)
            reflectiveQuadToRelative(3.63f, 0.73f)
            reflectiveQuadToRelative(2.95f, 1.99f)
            reflectiveQuadToRelative(1.99f, 2.95f)
            reflectiveQuadTo(21.3f, 12f)
            reflectiveQuadToRelative(-0.73f, 3.63f)
            reflectiveQuadToRelative(-1.99f, 2.95f)
            reflectiveQuadToRelative(-2.95f, 1.99f)
            reflectiveQuadTo(12f, 21.3f)
            reflectiveQuadTo(8.38f, 20.57f)
            close()
            moveToRelative(4.47f, -0.73f)
            quadToRelative(2.91f, -0.28f, 4.98f, -2.49f)
            reflectiveQuadTo(19.9f, 12f)
            quadToRelative(0f, -3.12f, -2.03f, -5.31f)
            reflectiveQuadTo(12.85f, 4.17f)
            verticalLineTo(19.84f)
            close()
          }
        }
        .build().also { _contrastW300 = it }
        IconWeight.W400 -> _contrastW400 ?: ImageVector.Builder(
          name = "contrast",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            close()
            moveTo(13f, 19.93f)
            quadToRelative(2.98f, -0.38f, 4.99f, -2.61f)
            reflectiveQuadTo(20f, 12f)
            quadTo(20f, 8.92f, 17.99f, 6.69f)
            reflectiveQuadTo(13f, 4.07f)
            verticalLineTo(19.93f)
            close()
          }
        }
        .build().also { _contrastW400 = it }
        IconWeight.W500 -> _contrastW500 ?: ImageVector.Builder(
          name = "contrast",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.02f, 21.4f)
            quadTo(6.16f, 20.6f, 4.78f, 19.22f)
            reflectiveQuadTo(2.6f, 15.98f)
            reflectiveQuadTo(1.8f, 12f)
            reflectiveQuadTo(2.6f, 8.02f)
            reflectiveQuadTo(4.78f, 4.78f)
            reflectiveQuadTo(8.02f, 2.6f)
            reflectiveQuadTo(12f, 1.8f)
            reflectiveQuadToRelative(3.98f, 0.8f)
            reflectiveQuadToRelative(3.24f, 2.18f)
            reflectiveQuadTo(21.4f, 8.02f)
            reflectiveQuadTo(22.2f, 12f)
            reflectiveQuadToRelative(-0.8f, 3.98f)
            reflectiveQuadToRelative(-2.18f, 3.24f)
            reflectiveQuadTo(15.98f, 21.4f)
            reflectiveQuadTo(12f, 22.2f)
            reflectiveQuadTo(8.02f, 21.4f)
            close()
            moveToRelative(5.05f, -1.56f)
            quadToRelative(2.93f, -0.4f, 4.89f, -2.6f)
            reflectiveQuadTo(19.93f, 12f)
            quadToRelative(0f, -3.03f, -1.96f, -5.23f)
            reflectiveQuadTo(13.07f, 4.16f)
            verticalLineTo(19.84f)
            close()
          }
        }
        .build().also { _contrastW500 = it }
    }

private var _contrastW300: ImageVector? = null
private var _contrastW400: ImageVector? = null
private var _contrastW500: ImageVector? = null

public val FujiIcons.Deblur: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _deblurW300 ?: ImageVector.Builder(
          name = "deblur",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.43f, 14.61f)
            quadTo(5.17f, 14.35f, 5.17f, 13.96f)
            reflectiveQuadTo(5.43f, 13.32f)
            reflectiveQuadTo(6.07f, 13.06f)
            reflectiveQuadToRelative(0.65f, 0.26f)
            reflectiveQuadToRelative(0.26f, 0.64f)
            reflectiveQuadTo(6.72f, 14.6f)
            reflectiveQuadTo(6.07f, 14.86f)
            reflectiveQuadTo(5.43f, 14.61f)
            close()
            moveToRelative(0f, 3.96f)
            quadTo(5.17f, 18.31f, 5.17f, 17.92f)
            reflectiveQuadTo(5.43f, 17.27f)
            reflectiveQuadTo(6.07f, 17.02f)
            reflectiveQuadToRelative(0.65f, 0.26f)
            reflectiveQuadToRelative(0.26f, 0.64f)
            reflectiveQuadTo(6.72f, 18.56f)
            reflectiveQuadTo(6.07f, 18.82f)
            reflectiveQuadTo(5.43f, 18.56f)
            close()
            moveToRelative(0f, -7.9f)
            quadTo(5.17f, 10.41f, 5.17f, 10.02f)
            reflectiveQuadTo(5.43f, 9.38f)
            reflectiveQuadTo(6.07f, 9.12f)
            reflectiveQuadTo(6.72f, 9.38f)
            reflectiveQuadToRelative(0.26f, 0.64f)
            reflectiveQuadTo(6.72f, 10.66f)
            reflectiveQuadTo(6.07f, 10.92f)
            reflectiveQuadTo(5.43f, 10.67f)
            close()
            moveTo(2.76f, 10.35f)
            quadTo(2.61f, 10.2f, 2.61f, 10f)
            reflectiveQuadTo(2.76f, 9.65f)
            reflectiveQuadTo(3.11f, 9.5f)
            reflectiveQuadTo(3.46f, 9.65f)
            reflectiveQuadTo(3.61f, 10f)
            reflectiveQuadTo(3.46f, 10.35f)
            reflectiveQuadTo(3.11f, 10.5f)
            reflectiveQuadTo(2.76f, 10.35f)
            close()
            moveTo(5.43f, 6.71f)
            quadTo(5.17f, 6.45f, 5.17f, 6.06f)
            reflectiveQuadTo(5.43f, 5.42f)
            reflectiveQuadTo(6.07f, 5.16f)
            reflectiveQuadTo(6.72f, 5.42f)
            reflectiveQuadTo(6.98f, 6.06f)
            reflectiveQuadTo(6.72f, 6.7f)
            reflectiveQuadTo(6.07f, 6.96f)
            reflectiveQuadTo(5.43f, 6.71f)
            close()
            moveToRelative(-2.67f, 7.6f)
            quadTo(2.61f, 14.16f, 2.61f, 13.96f)
            reflectiveQuadTo(2.76f, 13.61f)
            reflectiveQuadTo(3.11f, 13.46f)
            reflectiveQuadToRelative(0.35f, 0.15f)
            reflectiveQuadToRelative(0.15f, 0.35f)
            reflectiveQuadTo(3.46f, 14.31f)
            reflectiveQuadTo(3.11f, 14.46f)
            reflectiveQuadTo(2.76f, 14.31f)
            close()
            moveToRelative(6.92f, 6.92f)
            quadTo(9.53f, 21.08f, 9.53f, 20.88f)
            reflectiveQuadTo(9.68f, 20.53f)
            reflectiveQuadToRelative(0.35f, -0.15f)
            reflectiveQuadToRelative(0.35f, 0.15f)
            reflectiveQuadToRelative(0.15f, 0.35f)
            reflectiveQuadToRelative(-0.15f, 0.35f)
            reflectiveQuadToRelative(-0.35f, 0.15f)
            reflectiveQuadTo(9.68f, 21.23f)
            close()
            moveToRelative(0f, -17.78f)
            quadTo(9.53f, 3.3f, 9.53f, 3.1f)
            reflectiveQuadTo(9.68f, 2.75f)
            reflectiveQuadTo(10.03f, 2.6f)
            reflectiveQuadToRelative(0.35f, 0.15f)
            reflectiveQuadTo(10.53f, 3.1f)
            reflectiveQuadTo(10.38f, 3.45f)
            reflectiveQuadTo(10.03f, 3.6f)
            reflectiveQuadTo(9.68f, 3.45f)
            close()
            moveTo(9.39f, 6.71f)
            quadTo(9.13f, 6.45f, 9.13f, 6.06f)
            reflectiveQuadTo(9.39f, 5.42f)
            reflectiveQuadTo(10.03f, 5.16f)
            reflectiveQuadToRelative(0.65f, 0.26f)
            reflectiveQuadToRelative(0.26f, 0.64f)
            reflectiveQuadTo(10.68f, 6.7f)
            reflectiveQuadTo(10.03f, 6.96f)
            reflectiveQuadTo(9.39f, 6.71f)
            close()
            moveTo(9.11f, 14.88f)
            quadTo(8.73f, 14.5f, 8.73f, 13.96f)
            reflectiveQuadTo(9.11f, 13.04f)
            reflectiveQuadToRelative(0.92f, -0.38f)
            reflectiveQuadToRelative(0.93f, 0.38f)
            reflectiveQuadToRelative(0.38f, 0.92f)
            reflectiveQuadToRelative(-0.38f, 0.92f)
            reflectiveQuadToRelative(-0.92f, 0.38f)
            reflectiveQuadTo(9.11f, 14.88f)
            close()
            moveToRelative(0f, -3.94f)
            quadTo(8.73f, 10.56f, 8.73f, 10.02f)
            reflectiveQuadTo(9.11f, 9.1f)
            reflectiveQuadTo(10.03f, 8.71f)
            reflectiveQuadTo(10.95f, 9.1f)
            reflectiveQuadToRelative(0.38f, 0.92f)
            reflectiveQuadToRelative(-0.38f, 0.92f)
            reflectiveQuadToRelative(-0.92f, 0.38f)
            reflectiveQuadTo(9.11f, 10.94f)
            close()
            moveToRelative(0.28f, 7.62f)
            quadTo(9.13f, 18.31f, 9.13f, 17.92f)
            reflectiveQuadTo(9.39f, 17.27f)
            reflectiveQuadToRelative(0.64f, -0.26f)
            reflectiveQuadToRelative(0.65f, 0.26f)
            reflectiveQuadToRelative(0.26f, 0.64f)
            reflectiveQuadToRelative(-0.26f, 0.64f)
            reflectiveQuadToRelative(-0.65f, 0.26f)
            reflectiveQuadTo(9.39f, 18.56f)
            close()
            moveTo(17.49f, 7.15f)
            quadTo(15.68f, 5.11f, 13.02f, 4.76f)
            quadTo(12.71f, 4.73f, 12.51f, 4.52f)
            reflectiveQuadTo(12.3f, 4.01f)
            reflectiveQuadToRelative(0.22f, -0.5f)
            reflectiveQuadTo(13.03f, 3.35f)
            quadToRelative(3.27f, 0.38f, 5.47f, 2.83f)
            reflectiveQuadTo(20.7f, 12f)
            reflectiveQuadTo(18.5f, 17.82f)
            reflectiveQuadToRelative(-5.47f, 2.83f)
            quadToRelative(-0.29f, 0.04f, -0.51f, -0.16f)
            reflectiveQuadTo(12.3f, 19.99f)
            reflectiveQuadToRelative(0.21f, -0.51f)
            reflectiveQuadToRelative(0.51f, -0.25f)
            quadToRelative(2.66f, -0.34f, 4.47f, -2.38f)
            reflectiveQuadTo(19.3f, 12f)
            reflectiveQuadTo(17.49f, 7.15f)
            close()
            moveTo(12.3f, 12f)
            close()
          }
        }
        .build().also { _deblurW300 = it }
        IconWeight.W400 -> _deblurW400 ?: ImageVector.Builder(
          name = "deblur",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.29f, 14.71f)
            quadTo(5f, 14.43f, 5f, 14f)
            reflectiveQuadTo(5.29f, 13.29f)
            reflectiveQuadTo(6f, 13f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(7f, 14f)
            reflectiveQuadTo(6.71f, 14.71f)
            reflectiveQuadTo(6f, 15f)
            quadTo(5.58f, 15f, 5.29f, 14.71f)
            close()
            moveToRelative(0f, 4f)
            quadTo(5f, 18.43f, 5f, 18f)
            reflectiveQuadTo(5.29f, 17.29f)
            reflectiveQuadTo(6f, 17f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(7f, 18f)
            reflectiveQuadTo(6.71f, 18.71f)
            reflectiveQuadTo(6f, 19f)
            quadTo(5.58f, 19f, 5.29f, 18.71f)
            close()
            moveToRelative(0f, -8f)
            quadTo(5f, 10.43f, 5f, 10f)
            quadTo(5f, 9.57f, 5.29f, 9.29f)
            reflectiveQuadTo(6f, 9f)
            reflectiveQuadTo(6.71f, 9.29f)
            reflectiveQuadTo(7f, 10f)
            reflectiveQuadTo(6.71f, 10.71f)
            reflectiveQuadTo(6f, 11f)
            quadTo(5.58f, 11f, 5.29f, 10.71f)
            close()
            moveTo(2.65f, 10.35f)
            quadTo(2.5f, 10.2f, 2.5f, 10f)
            reflectiveQuadTo(2.65f, 9.65f)
            reflectiveQuadTo(3f, 9.5f)
            reflectiveQuadTo(3.35f, 9.65f)
            reflectiveQuadTo(3.5f, 10f)
            reflectiveQuadTo(3.35f, 10.35f)
            reflectiveQuadTo(3f, 10.5f)
            reflectiveQuadTo(2.65f, 10.35f)
            close()
            moveTo(5.29f, 6.71f)
            quadTo(5f, 6.43f, 5f, 6f)
            reflectiveQuadTo(5.29f, 5.29f)
            reflectiveQuadTo(6f, 5f)
            reflectiveQuadTo(6.71f, 5.29f)
            reflectiveQuadTo(7f, 6f)
            reflectiveQuadTo(6.71f, 6.71f)
            reflectiveQuadTo(6f, 7f)
            quadTo(5.58f, 7f, 5.29f, 6.71f)
            close()
            moveTo(2.65f, 14.35f)
            quadTo(2.5f, 14.2f, 2.5f, 14f)
            reflectiveQuadTo(2.65f, 13.65f)
            reflectiveQuadTo(3f, 13.5f)
            reflectiveQuadToRelative(0.35f, 0.15f)
            reflectiveQuadTo(3.5f, 14f)
            reflectiveQuadTo(3.35f, 14.35f)
            reflectiveQuadTo(3f, 14.5f)
            reflectiveQuadTo(2.65f, 14.35f)
            close()
            moveToRelative(7f, 7f)
            quadTo(9.5f, 21.2f, 9.5f, 21f)
            reflectiveQuadTo(9.65f, 20.65f)
            reflectiveQuadTo(10f, 20.5f)
            reflectiveQuadToRelative(0.35f, 0.15f)
            reflectiveQuadTo(10.5f, 21f)
            reflectiveQuadToRelative(-0.15f, 0.35f)
            reflectiveQuadTo(10f, 21.5f)
            reflectiveQuadTo(9.65f, 21.35f)
            close()
            moveToRelative(0f, -18f)
            quadTo(9.5f, 3.2f, 9.5f, 3f)
            reflectiveQuadTo(9.65f, 2.65f)
            reflectiveQuadTo(10f, 2.5f)
            reflectiveQuadToRelative(0.35f, 0.15f)
            reflectiveQuadTo(10.5f, 3f)
            reflectiveQuadTo(10.35f, 3.35f)
            reflectiveQuadTo(10f, 3.5f)
            reflectiveQuadTo(9.65f, 3.35f)
            close()
            moveTo(9.29f, 6.71f)
            quadTo(9f, 6.43f, 9f, 6f)
            reflectiveQuadTo(9.29f, 5.29f)
            quadTo(9.58f, 5f, 10f, 5f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(11f, 6f)
            reflectiveQuadTo(10.71f, 6.71f)
            reflectiveQuadTo(10f, 7f)
            quadTo(9.58f, 7f, 9.29f, 6.71f)
            close()
            moveTo(8.94f, 15.06f)
            quadTo(8.5f, 14.63f, 8.5f, 14f)
            reflectiveQuadTo(8.94f, 12.94f)
            reflectiveQuadTo(10f, 12.5f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(11.5f, 14f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(10f, 15.5f)
            reflectiveQuadTo(8.94f, 15.06f)
            close()
            moveToRelative(0f, -4f)
            quadTo(8.5f, 10.63f, 8.5f, 10f)
            reflectiveQuadTo(8.94f, 8.94f)
            reflectiveQuadTo(10f, 8.5f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(11.5f, 10f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(10f, 11.5f)
            reflectiveQuadTo(8.94f, 11.06f)
            close()
            moveToRelative(0.35f, 7.65f)
            quadTo(9f, 18.43f, 9f, 18f)
            reflectiveQuadTo(9.29f, 17.29f)
            quadTo(9.58f, 17f, 10f, 17f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(11f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(10f, 19f)
            quadTo(9.58f, 19f, 9.29f, 18.71f)
            close()
            moveTo(17.28f, 7.4f)
            quadTo(15.55f, 5.45f, 13f, 5.07f)
            quadTo(12.58f, 5.02f, 12.29f, 4.72f)
            reflectiveQuadTo(12f, 4f)
            quadTo(12f, 3.57f, 12.3f, 3.29f)
            reflectiveQuadTo(13.03f, 3.05f)
            quadToRelative(3.38f, 0.4f, 5.67f, 2.94f)
            quadTo(21f, 8.52f, 21f, 12f)
            reflectiveQuadToRelative(-2.3f, 6.01f)
            reflectiveQuadToRelative(-5.67f, 2.94f)
            quadTo(12.6f, 21f, 12.3f, 20.71f)
            quadTo(12f, 20.43f, 12f, 20f)
            reflectiveQuadToRelative(0.29f, -0.73f)
            quadTo(12.58f, 18.98f, 13f, 18.93f)
            quadToRelative(2.55f, -0.38f, 4.28f, -2.32f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadTo(17.28f, 7.4f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build().also { _deblurW400 = it }
        IconWeight.W500 -> _deblurW500 ?: ImageVector.Builder(
          name = "deblur",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.25f, 14.75f)
            quadTo(4.95f, 14.44f, 4.95f, 14f)
            reflectiveQuadToRelative(0.3f, -0.75f)
            reflectiveQuadTo(6f, 12.95f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadTo(7.05f, 14f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadTo(6f, 15.05f)
            reflectiveQuadTo(5.25f, 14.75f)
            close()
            moveToRelative(0f, 4f)
            quadTo(4.95f, 18.44f, 4.95f, 18f)
            reflectiveQuadToRelative(0.3f, -0.75f)
            reflectiveQuadTo(6f, 16.95f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadTo(7.05f, 18f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadTo(6f, 19.05f)
            reflectiveQuadTo(5.25f, 18.75f)
            close()
            moveToRelative(0f, -8f)
            quadTo(4.95f, 10.44f, 4.95f, 10f)
            reflectiveQuadTo(5.25f, 9.25f)
            reflectiveQuadTo(6f, 8.95f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadTo(7.05f, 10f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadTo(6f, 11.05f)
            reflectiveQuadTo(5.25f, 10.75f)
            close()
            moveTo(2.61f, 10.39f)
            quadTo(2.45f, 10.22f, 2.45f, 10f)
            reflectiveQuadTo(2.61f, 9.61f)
            reflectiveQuadTo(3f, 9.45f)
            reflectiveQuadTo(3.39f, 9.61f)
            reflectiveQuadTo(3.55f, 10f)
            reflectiveQuadTo(3.39f, 10.39f)
            reflectiveQuadTo(3f, 10.55f)
            reflectiveQuadTo(2.61f, 10.39f)
            close()
            moveTo(5.25f, 6.75f)
            quadTo(4.95f, 6.44f, 4.95f, 6f)
            reflectiveQuadTo(5.25f, 5.25f)
            reflectiveQuadTo(6f, 4.95f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadTo(7.05f, 6f)
            reflectiveQuadTo(6.75f, 6.75f)
            reflectiveQuadTo(6f, 7.05f)
            reflectiveQuadTo(5.25f, 6.75f)
            close()
            moveTo(2.61f, 14.39f)
            quadTo(2.45f, 14.22f, 2.45f, 14f)
            reflectiveQuadTo(2.61f, 13.61f)
            reflectiveQuadTo(3f, 13.45f)
            reflectiveQuadToRelative(0.39f, 0.16f)
            reflectiveQuadTo(3.55f, 14f)
            reflectiveQuadTo(3.39f, 14.39f)
            reflectiveQuadTo(3f, 14.55f)
            reflectiveQuadTo(2.61f, 14.39f)
            close()
            moveToRelative(7f, 7f)
            quadTo(9.45f, 21.22f, 9.45f, 21f)
            reflectiveQuadTo(9.61f, 20.61f)
            reflectiveQuadTo(10f, 20.45f)
            reflectiveQuadToRelative(0.39f, 0.16f)
            reflectiveQuadTo(10.55f, 21f)
            reflectiveQuadToRelative(-0.16f, 0.39f)
            reflectiveQuadTo(10f, 21.55f)
            reflectiveQuadTo(9.61f, 21.39f)
            close()
            moveToRelative(0f, -18f)
            quadTo(9.45f, 3.22f, 9.45f, 3f)
            reflectiveQuadTo(9.61f, 2.61f)
            reflectiveQuadTo(10f, 2.45f)
            reflectiveQuadToRelative(0.39f, 0.16f)
            reflectiveQuadTo(10.55f, 3f)
            reflectiveQuadTo(10.39f, 3.39f)
            reflectiveQuadTo(10f, 3.55f)
            reflectiveQuadTo(9.61f, 3.39f)
            close()
            moveTo(9.25f, 6.75f)
            quadTo(8.95f, 6.44f, 8.95f, 6f)
            reflectiveQuadTo(9.25f, 5.25f)
            reflectiveQuadTo(10f, 4.95f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadTo(11.05f, 6f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadTo(10f, 7.05f)
            reflectiveQuadTo(9.25f, 6.75f)
            close()
            moveTo(8.9f, 15.1f)
            quadTo(8.45f, 14.65f, 8.45f, 14f)
            reflectiveQuadTo(8.9f, 12.9f)
            reflectiveQuadTo(10f, 12.45f)
            reflectiveQuadToRelative(1.1f, 0.45f)
            reflectiveQuadTo(11.55f, 14f)
            reflectiveQuadTo(11.1f, 15.1f)
            reflectiveQuadTo(10f, 15.55f)
            reflectiveQuadTo(8.9f, 15.1f)
            close()
            moveToRelative(0f, -4f)
            quadTo(8.45f, 10.65f, 8.45f, 10f)
            reflectiveQuadTo(8.9f, 8.9f)
            reflectiveQuadTo(10f, 8.45f)
            reflectiveQuadTo(11.1f, 8.9f)
            reflectiveQuadTo(11.55f, 10f)
            reflectiveQuadTo(11.1f, 11.1f)
            reflectiveQuadTo(10f, 11.55f)
            reflectiveQuadTo(8.9f, 11.1f)
            close()
            moveToRelative(0.35f, 7.65f)
            quadTo(8.95f, 18.44f, 8.95f, 18f)
            reflectiveQuadToRelative(0.3f, -0.75f)
            reflectiveQuadTo(10f, 16.95f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadTo(11.05f, 18f)
            reflectiveQuadToRelative(-0.3f, 0.75f)
            reflectiveQuadTo(10f, 19.05f)
            reflectiveQuadTo(9.25f, 18.75f)
            close()
            moveTo(17.27f, 7.5f)
            quadTo(15.61f, 5.58f, 13.14f, 5.17f)
            quadTo(12.66f, 5.1f, 12.33f, 4.75f)
            reflectiveQuadTo(12f, 3.93f)
            reflectiveQuadTo(12.34f, 3.13f)
            reflectiveQuadTo(13.16f, 2.86f)
            quadToRelative(3.41f, 0.44f, 5.73f, 3.02f)
            reflectiveQuadTo(21.2f, 12f)
            reflectiveQuadToRelative(-2.31f, 6.11f)
            reflectiveQuadToRelative(-5.73f, 3.02f)
            quadTo(12.68f, 21.2f, 12.34f, 20.87f)
            reflectiveQuadTo(12f, 20.07f)
            reflectiveQuadToRelative(0.33f, -0.82f)
            reflectiveQuadToRelative(0.81f, -0.42f)
            quadToRelative(2.47f, -0.41f, 4.13f, -2.33f)
            reflectiveQuadTo(18.93f, 12f)
            reflectiveQuadTo(17.27f, 7.5f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build().also { _deblurW500 = it }
    }

private var _deblurW300: ImageVector? = null
private var _deblurW400: ImageVector? = null
private var _deblurW500: ImageVector? = null

public val FujiIcons.Delete: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _deleteW300 ?: ImageVector.Builder(
          name = "delete",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(7.37f, 20.3f)
            quadToRelative(-0.71f, 0f, -1.21f, -0.5f)
            reflectiveQuadTo(5.66f, 18.59f)
            verticalLineTo(6.06f)
            horizontalLineTo(5.38f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(4.68f, 5.36f)
            reflectiveQuadTo(4.89f, 4.87f)
            reflectiveQuadTo(5.38f, 4.66f)
            horizontalLineTo(9.04f)
            verticalLineTo(4.65f)
            quadToRelative(0f, -0.34f, 0.25f, -0.59f)
            reflectiveQuadTo(9.88f, 3.82f)
            horizontalLineToRelative(4.27f)
            quadToRelative(0.34f, 0f, 0.59f, 0.25f)
            reflectiveQuadToRelative(0.25f, 0.59f)
            verticalLineTo(4.66f)
            horizontalLineToRelative(3.66f)
            quadToRelative(0.28f, 0f, 0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            reflectiveQuadTo(19.13f, 5.86f)
            reflectiveQuadTo(18.64f, 6.06f)
            horizontalLineTo(18.36f)
            verticalLineTo(18.59f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(7.37f)
            close()
            moveTo(16.96f, 6.06f)
            horizontalLineTo(7.06f)
            verticalLineTo(18.59f)
            quadToRelative(0f, 0.13f, 0.09f, 0.22f)
            reflectiveQuadTo(7.37f, 18.9f)
            horizontalLineToRelative(9.28f)
            quadToRelative(0.13f, 0f, 0.22f, -0.09f)
            reflectiveQuadToRelative(0.09f, -0.22f)
            verticalLineTo(6.06f)
            close()
            moveTo(10.7f, 16.72f)
            quadToRelative(0.21f, -0.2f, 0.21f, -0.49f)
            verticalLineTo(8.73f)
            quadToRelative(0f, -0.29f, -0.21f, -0.5f)
            reflectiveQuadTo(10.2f, 8.03f)
            reflectiveQuadTo(9.71f, 8.23f)
            reflectiveQuadTo(9.5f, 8.73f)
            verticalLineToRelative(7.5f)
            quadToRelative(0f, 0.29f, 0.21f, 0.49f)
            reflectiveQuadToRelative(0.49f, 0.2f)
            reflectiveQuadToRelative(0.49f, -0.2f)
            close()
            moveToRelative(3.61f, 0f)
            quadToRelative(0.21f, -0.2f, 0.21f, -0.49f)
            verticalLineTo(8.73f)
            quadToRelative(0f, -0.29f, -0.21f, -0.5f)
            reflectiveQuadTo(13.82f, 8.03f)
            reflectiveQuadTo(13.32f, 8.23f)
            reflectiveQuadToRelative(-0.21f, 0.5f)
            verticalLineToRelative(7.5f)
            quadToRelative(0f, 0.29f, 0.21f, 0.49f)
            reflectiveQuadToRelative(0.49f, 0.2f)
            reflectiveQuadToRelative(0.49f, -0.2f)
            close()
            moveTo(7.06f, 6.06f)
            verticalLineTo(18.59f)
            quadToRelative(0f, 0.13f, 0f, 0.22f)
            reflectiveQuadToRelative(0f, 0.09f)
            quadToRelative(0f, 0f, 0f, -0.09f)
            reflectiveQuadToRelative(0f, -0.22f)
            verticalLineTo(6.06f)
            close()
          }
        }
        .build().also { _deleteW300 = it }
        IconWeight.W400 -> _deleteW400 ?: ImageVector.Builder(
          name = "delete",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(7f, 21f)
            quadTo(6.18f, 21f, 5.59f, 20.41f)
            reflectiveQuadTo(5f, 19f)
            verticalLineTo(6f)
            quadTo(4.58f, 6f, 4.29f, 5.71f)
            quadTo(4f, 5.43f, 4f, 5f)
            reflectiveQuadTo(4.29f, 4.29f)
            reflectiveQuadTo(5f, 4f)
            horizontalLineTo(9f)
            quadTo(9f, 3.57f, 9.29f, 3.29f)
            quadTo(9.58f, 3f, 10f, 3f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 4f)
            horizontalLineToRelative(4f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(20f, 5f)
            reflectiveQuadTo(19.71f, 5.71f)
            reflectiveQuadTo(19f, 6f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(17f, 21f)
            horizontalLineTo(7f)
            close()
            moveTo(17f, 6f)
            horizontalLineTo(7f)
            verticalLineTo(19f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            close()
            moveTo(10.71f, 16.71f)
            quadTo(11f, 16.43f, 11f, 16f)
            verticalLineTo(9f)
            quadTo(11f, 8.57f, 10.71f, 8.29f)
            reflectiveQuadTo(10f, 8f)
            quadTo(9.58f, 8f, 9.29f, 8.29f)
            reflectiveQuadTo(9f, 9f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            quadTo(9.58f, 17f, 10f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(4f, 0f)
            quadTo(15f, 16.43f, 15f, 16f)
            verticalLineTo(9f)
            quadTo(15f, 8.57f, 14.71f, 8.29f)
            reflectiveQuadTo(14f, 8f)
            reflectiveQuadTo(13.29f, 8.29f)
            reflectiveQuadTo(13f, 9f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            reflectiveQuadTo(14f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveTo(7f, 6f)
            verticalLineTo(19f)
            verticalLineTo(6f)
            close()
          }
        }
        .build().also { _deleteW400 = it }
        IconWeight.W500 -> _deleteW500 ?: ImageVector.Builder(
          name = "delete",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6.93f, 21.2f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(4.66f, 18.93f)
            verticalLineTo(6.07f)
            quadTo(4.18f, 6.07f, 3.85f, 5.74f)
            reflectiveQuadTo(3.52f, 4.93f)
            reflectiveQuadTo(3.85f, 4.12f)
            reflectiveQuadTo(4.66f, 3.79f)
            horizontalLineToRelative(4.2f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(10f, 2.65f)
            horizontalLineToRelative(3.99f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            horizontalLineToRelative(4.22f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            reflectiveQuadTo(20.15f, 5.74f)
            reflectiveQuadTo(19.34f, 6.07f)
            verticalLineTo(18.93f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(17.07f, 21.2f)
            horizontalLineTo(6.93f)
            close()
            moveTo(17.07f, 6.07f)
            horizontalLineTo(6.93f)
            verticalLineTo(18.93f)
            horizontalLineTo(17.07f)
            verticalLineTo(6.07f)
            close()
            moveTo(10.72f, 16.69f)
            quadToRelative(0.31f, -0.31f, 0.31f, -0.76f)
            verticalLineTo(9.06f)
            quadToRelative(0f, -0.45f, -0.31f, -0.76f)
            reflectiveQuadTo(9.96f, 7.99f)
            reflectiveQuadTo(9.2f, 8.3f)
            reflectiveQuadTo(8.89f, 9.06f)
            verticalLineToRelative(6.87f)
            quadToRelative(0f, 0.45f, 0.31f, 0.76f)
            reflectiveQuadToRelative(0.76f, 0.31f)
            reflectiveQuadToRelative(0.76f, -0.31f)
            close()
            moveToRelative(4.08f, 0f)
            quadToRelative(0.31f, -0.31f, 0.31f, -0.76f)
            verticalLineTo(9.06f)
            quadTo(15.11f, 8.61f, 14.8f, 8.3f)
            reflectiveQuadTo(14.04f, 7.99f)
            reflectiveQuadTo(13.28f, 8.3f)
            reflectiveQuadTo(12.97f, 9.06f)
            verticalLineToRelative(6.87f)
            quadToRelative(0f, 0.45f, 0.31f, 0.76f)
            reflectiveQuadToRelative(0.76f, 0.31f)
            reflectiveQuadTo(14.8f, 16.69f)
            close()
            moveTo(6.93f, 6.07f)
            verticalLineTo(18.93f)
            verticalLineTo(6.07f)
            close()
          }
        }
        .build().also { _deleteW500 = it }
    }

private var _deleteW300: ImageVector? = null
private var _deleteW400: ImageVector? = null
private var _deleteW500: ImageVector? = null

public val FujiIcons.Details: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _detailsW300 ?: ImageVector.Builder(
          name = "details",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.4f, 20.45f)
            quadToRelative(-0.49f, 0f, -0.74f, -0.42f)
            reflectiveQuadTo(3.65f, 19.18f)
            lineTo(11.25f, 5.49f)
            quadTo(11.49f, 5.05f, 12f, 5.05f)
            reflectiveQuadToRelative(0.75f, 0.44f)
            lineToRelative(7.61f, 13.69f)
            quadToRelative(0.23f, 0.42f, -0.01f, 0.85f)
            reflectiveQuadToRelative(-0.74f, 0.42f)
            horizontalLineTo(4.4f)
            close()
            moveToRelative(0.92f, -1.4f)
            horizontalLineTo(11.3f)
            verticalLineTo(8.29f)
            lineTo(5.32f, 19.05f)
            close()
            moveToRelative(7.38f, 0f)
            horizontalLineToRelative(5.98f)
            lineTo(12.7f, 8.29f)
            verticalLineTo(19.05f)
            close()
          }
        }
        .build().also { _detailsW300 = it }
        IconWeight.W400 -> _detailsW400 ?: ImageVector.Builder(
          name = "details",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(3.7f, 21f)
            quadTo(3.13f, 21f, 2.84f, 20.51f)
            reflectiveQuadTo(2.83f, 19.52f)
            lineTo(11.13f, 4.57f)
            quadTo(11.4f, 4.07f, 12f, 4.07f)
            reflectiveQuadToRelative(0.88f, 0.5f)
            lineToRelative(8.3f, 14.95f)
            quadToRelative(0.28f, 0.5f, -0.01f, 0.99f)
            reflectiveQuadTo(20.3f, 21f)
            horizontalLineTo(3.7f)
            close()
            moveTo(5.4f, 19f)
            horizontalLineTo(11f)
            verticalLineTo(8.92f)
            lineTo(5.4f, 19f)
            close()
            moveTo(13f, 19f)
            horizontalLineToRelative(5.6f)
            lineTo(13f, 8.92f)
            verticalLineTo(19f)
            close()
          }
        }
        .build().also { _detailsW400 = it }
        IconWeight.W500 -> _detailsW500 ?: ImageVector.Builder(
          name = "details",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(3.57f, 21.2f)
            quadToRelative(-0.65f, 0f, -0.98f, -0.56f)
            reflectiveQuadTo(2.57f, 19.52f)
            lineTo(11.01f, 4.34f)
            quadTo(11.32f, 3.77f, 12f, 3.77f)
            reflectiveQuadToRelative(0.99f, 0.57f)
            lineToRelative(8.43f, 15.18f)
            quadToRelative(0.31f, 0.57f, -0.02f, 1.13f)
            reflectiveQuadTo(20.43f, 21.2f)
            horizontalLineTo(3.57f)
            close()
            moveTo(5.5f, 18.93f)
            horizontalLineToRelative(5.36f)
            verticalLineTo(9.28f)
            lineTo(5.5f, 18.93f)
            close()
            moveToRelative(7.64f, 0f)
            horizontalLineTo(18.5f)
            lineTo(13.14f, 9.28f)
            verticalLineToRelative(9.64f)
            close()
          }
        }
        .build().also { _detailsW500 = it }
    }

private var _detailsW300: ImageVector? = null
private var _detailsW400: ImageVector? = null
private var _detailsW500: ImageVector? = null

public val FujiIcons.Diamond: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _diamondW300 ?: ImageVector.Builder(
          name = "diamond",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.29f, 19.41f)
            quadToRelative(-0.34f, -0.16f, -0.6f, -0.46f)
            lineTo(3.07f, 9.8f)
            quadTo(2.87f, 9.57f, 2.77f, 9.3f)
            reflectiveQuadTo(2.68f, 8.71f)
            quadToRelative(0f, -0.19f, 0.04f, -0.38f)
            reflectiveQuadTo(2.86f, 7.95f)
            lineTo(4.71f, 4.24f)
            quadTo(4.94f, 3.81f, 5.34f, 3.56f)
            reflectiveQuadTo(6.24f, 3.3f)
            horizontalLineTo(17.77f)
            quadToRelative(0.49f, 0f, 0.89f, 0.26f)
            reflectiveQuadToRelative(0.63f, 0.69f)
            lineToRelative(1.86f, 3.71f)
            quadToRelative(0.09f, 0.19f, 0.14f, 0.38f)
            reflectiveQuadToRelative(0.04f, 0.38f)
            quadToRelative(0f, 0.31f, -0.1f, 0.58f)
            reflectiveQuadTo(20.93f, 9.8f)
            lineToRelative(-7.62f, 9.14f)
            quadToRelative(-0.26f, 0.31f, -0.61f, 0.46f)
            reflectiveQuadTo(12f, 19.57f)
            reflectiveQuadTo(11.29f, 19.41f)
            close()
            moveTo(9.14f, 8.3f)
            horizontalLineToRelative(5.72f)
            lineTo(13.06f, 4.7f)
            horizontalLineTo(10.94f)
            lineTo(9.14f, 8.3f)
            close()
            moveToRelative(2.16f, 9.21f)
            verticalLineTo(9.7f)
            horizontalLineTo(4.81f)
            lineToRelative(6.49f, 7.81f)
            close()
            moveToRelative(1.4f, 0f)
            lineTo(19.19f, 9.7f)
            horizontalLineTo(12.7f)
            verticalLineToRelative(7.81f)
            close()
            moveTo(16.42f, 8.3f)
            horizontalLineToRelative(3.32f)
            lineTo(18.02f, 4.87f)
            quadTo(17.98f, 4.79f, 17.91f, 4.75f)
            reflectiveQuadTo(17.74f, 4.7f)
            horizontalLineTo(14.62f)
            lineToRelative(1.8f, 3.6f)
            close()
            moveTo(4.27f, 8.3f)
            horizontalLineTo(7.59f)
            lineTo(9.38f, 4.7f)
            horizontalLineTo(6.26f)
            quadTo(6.17f, 4.7f, 6.09f, 4.75f)
            reflectiveQuadTo(5.98f, 4.87f)
            lineTo(4.27f, 8.3f)
            close()
          }
        }
        .build().also { _diamondW300 = it }
        IconWeight.W400 -> _diamondW400 ?: ImageVector.Builder(
          name = "diamond",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.18f, 19.69f)
            quadToRelative(-0.4f, -0.19f, -0.7f, -0.54f)
            lineTo(2.83f, 10f)
            quadTo(2.6f, 9.73f, 2.49f, 9.4f)
            reflectiveQuadTo(2.38f, 8.73f)
            quadTo(2.38f, 8.5f, 2.41f, 8.26f)
            reflectiveQuadTo(2.58f, 7.82f)
            lineTo(4.45f, 4.1f)
            quadTo(4.73f, 3.6f, 5.19f, 3.3f)
            reflectiveQuadTo(6.23f, 3f)
            horizontalLineTo(17.78f)
            quadToRelative(0.57f, 0f, 1.04f, 0.3f)
            quadToRelative(0.46f, 0.3f, 0.74f, 0.8f)
            lineToRelative(1.88f, 3.73f)
            quadToRelative(0.13f, 0.2f, 0.16f, 0.44f)
            reflectiveQuadToRelative(0.04f, 0.46f)
            quadToRelative(0f, 0.35f, -0.11f, 0.67f)
            quadTo(21.4f, 9.73f, 21.18f, 10f)
            lineToRelative(-7.65f, 9.15f)
            quadToRelative(-0.3f, 0.35f, -0.7f, 0.54f)
            reflectiveQuadTo(12f, 19.88f)
            reflectiveQuadTo(11.18f, 19.69f)
            close()
            moveTo(9.63f, 8f)
            horizontalLineToRelative(4.75f)
            lineTo(12.88f, 5f)
            horizontalLineTo(11.13f)
            lineTo(9.63f, 8f)
            close()
            moveTo(11f, 16.68f)
            verticalLineTo(10f)
            horizontalLineTo(5.45f)
            lineTo(11f, 16.68f)
            close()
            moveToRelative(2f, 0f)
            lineTo(18.55f, 10f)
            horizontalLineTo(13f)
            verticalLineToRelative(6.68f)
            close()
            moveTo(16.6f, 8f)
            horizontalLineToRelative(2.65f)
            lineTo(17.75f, 5f)
            horizontalLineTo(15.1f)
            lineToRelative(1.5f, 3f)
            close()
            moveTo(4.75f, 8f)
            horizontalLineTo(7.4f)
            lineTo(8.9f, 5f)
            horizontalLineTo(6.25f)
            lineTo(4.75f, 8f)
            close()
          }
        }
        .build().also { _diamondW400 = it }
        IconWeight.W500 -> _diamondW500 ?: ImageVector.Builder(
          name = "diamond",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.06f, 19.65f)
            quadTo(10.6f, 19.45f, 10.26f, 19.04f)
            lineTo(2.84f, 10.15f)
            quadTo(2.58f, 9.84f, 2.45f, 9.47f)
            reflectiveQuadTo(2.32f, 8.7f)
            quadToRelative(0f, -0.25f, 0.05f, -0.52f)
            reflectiveQuadTo(2.55f, 7.68f)
            lineTo(4.32f, 4.16f)
            quadTo(4.62f, 3.59f, 5.16f, 3.25f)
            reflectiveQuadTo(6.34f, 2.91f)
            horizontalLineTo(17.66f)
            quadToRelative(0.65f, 0f, 1.19f, 0.34f)
            reflectiveQuadToRelative(0.84f, 0.91f)
            lineToRelative(1.77f, 3.52f)
            quadToRelative(0.14f, 0.24f, 0.18f, 0.5f)
            reflectiveQuadTo(21.68f, 8.7f)
            quadToRelative(0f, 0.4f, -0.13f, 0.77f)
            reflectiveQuadToRelative(-0.39f, 0.68f)
            lineToRelative(-7.42f, 8.89f)
            quadToRelative(-0.34f, 0.4f, -0.8f, 0.61f)
            reflectiveQuadTo(12f, 19.86f)
            reflectiveQuadTo(11.06f, 19.65f)
            close()
            moveTo(9.62f, 8f)
            horizontalLineToRelative(4.76f)
            lineTo(12.92f, 5.09f)
            horizontalLineTo(11.08f)
            lineTo(9.62f, 8f)
            close()
            moveTo(11f, 16.53f)
            verticalLineTo(10f)
            horizontalLineTo(5.57f)
            lineTo(11f, 16.53f)
            close()
            moveToRelative(2f, 0f)
            lineTo(18.43f, 10f)
            horizontalLineTo(13f)
            verticalLineToRelative(6.53f)
            close()
            moveTo(16.61f, 8f)
            horizontalLineToRelative(2.54f)
            lineTo(17.7f, 5.09f)
            horizontalLineTo(15.15f)
            lineTo(16.61f, 8f)
            close()
            moveTo(4.85f, 8f)
            horizontalLineTo(7.39f)
            lineTo(8.85f, 5.09f)
            horizontalLineTo(6.3f)
            lineTo(4.85f, 8f)
            close()
          }
        }
        .build().also { _diamondW500 = it }
    }

private var _diamondW300: ImageVector? = null
private var _diamondW400: ImageVector? = null
private var _diamondW500: ImageVector? = null

public val FujiIcons.DiscoverTune: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _discoverTuneW300 ?: ImageVector.Builder(
          name = "discover_tune",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(13.9f, 8.7f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.2f)
            reflectiveQuadTo(13.2f, 8f)
            reflectiveQuadToRelative(0.2f, -0.5f)
            reflectiveQuadTo(13.9f, 7.3f)
            horizontalLineToRelative(2.3f)
            verticalLineTo(4.1f)
            quadToRelative(0f, -0.29f, 0.2f, -0.5f)
            reflectiveQuadTo(16.9f, 3.4f)
            reflectiveQuadTo(17.4f, 3.6f)
            reflectiveQuadTo(17.6f, 4.1f)
            verticalLineTo(7.3f)
            horizontalLineToRelative(2.3f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadTo(20.6f, 8f)
            reflectiveQuadTo(20.4f, 8.49f)
            reflectiveQuadTo(19.9f, 8.7f)
            horizontalLineToRelative(-6f)
            close()
            moveToRelative(2.51f, 11.7f)
            quadTo(16.2f, 20.19f, 16.2f, 19.9f)
            verticalLineTo(12f)
            quadToRelative(0f, -0.29f, 0.2f, -0.5f)
            reflectiveQuadTo(16.9f, 11.3f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            reflectiveQuadTo(17.6f, 12f)
            verticalLineToRelative(7.9f)
            quadToRelative(0f, 0.29f, -0.21f, 0.49f)
            reflectiveQuadToRelative(-0.5f, 0.2f)
            reflectiveQuadTo(16.4f, 20.39f)
            close()
            moveToRelative(-9.8f, 0f)
            quadTo(6.4f, 20.19f, 6.4f, 19.9f)
            verticalLineTo(16.7f)
            horizontalLineTo(4.1f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.2f)
            reflectiveQuadTo(3.4f, 16f)
            reflectiveQuadToRelative(0.2f, -0.5f)
            reflectiveQuadTo(4.1f, 15.3f)
            horizontalLineToRelative(6f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadTo(10.8f, 16f)
            reflectiveQuadTo(10.6f, 16.49f)
            reflectiveQuadToRelative(-0.5f, 0.2f)
            horizontalLineTo(7.8f)
            verticalLineToRelative(3.2f)
            quadToRelative(0f, 0.29f, -0.21f, 0.49f)
            reflectiveQuadTo(7.1f, 20.6f)
            reflectiveQuadTo(6.61f, 20.39f)
            close()
            moveToRelative(0f, -7.9f)
            quadTo(6.4f, 12.29f, 6.4f, 12f)
            verticalLineTo(4.1f)
            quadToRelative(0f, -0.29f, 0.2f, -0.5f)
            reflectiveQuadTo(7.1f, 3.4f)
            reflectiveQuadTo(7.6f, 3.6f)
            reflectiveQuadTo(7.8f, 4.1f)
            verticalLineTo(12f)
            quadToRelative(0f, 0.29f, -0.21f, 0.49f)
            reflectiveQuadTo(7.1f, 12.7f)
            reflectiveQuadTo(6.61f, 12.49f)
            close()
          }
        }
        .build().also { _discoverTuneW300 = it }
        IconWeight.W400 -> _discoverTuneW400 ?: ImageVector.Builder(
          name = "discover_tune",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(14f, 9f)
            quadTo(13.58f, 9f, 13.29f, 8.71f)
            reflectiveQuadTo(13f, 8f)
            quadTo(13f, 7.57f, 13.29f, 7.29f)
            reflectiveQuadTo(14f, 7f)
            horizontalLineToRelative(2f)
            verticalLineTo(4f)
            quadTo(16f, 3.57f, 16.29f, 3.29f)
            reflectiveQuadTo(17f, 3f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 4f)
            verticalLineTo(7f)
            horizontalLineToRelative(2f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(20f, 9f)
            horizontalLineTo(14f)
            close()
            moveToRelative(2.29f, 11.71f)
            quadTo(16f, 20.43f, 16f, 20f)
            verticalLineTo(12f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(17f, 11f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(18f, 12f)
            verticalLineToRelative(8f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(17f, 21f)
            reflectiveQuadTo(16.29f, 20.71f)
            close()
            moveToRelative(-10f, 0f)
            quadTo(6f, 20.43f, 6f, 20f)
            verticalLineTo(17f)
            horizontalLineTo(4f)
            quadTo(3.58f, 17f, 3.29f, 16.71f)
            quadTo(3f, 16.43f, 3f, 16f)
            reflectiveQuadTo(3.29f, 15.29f)
            reflectiveQuadTo(4f, 15f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(11f, 16f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(10f, 17f)
            horizontalLineTo(8f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(7f, 21f)
            quadTo(6.58f, 21f, 6.29f, 20.71f)
            close()
            moveToRelative(0f, -8f)
            quadTo(6f, 12.43f, 6f, 12f)
            verticalLineTo(4f)
            quadTo(6f, 3.57f, 6.29f, 3.29f)
            reflectiveQuadTo(7f, 3f)
            reflectiveQuadTo(7.71f, 3.29f)
            reflectiveQuadTo(8f, 4f)
            verticalLineToRelative(8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(7f, 13f)
            quadTo(6.58f, 13f, 6.29f, 12.71f)
            close()
          }
        }
        .build().also { _discoverTuneW400 = it }
        IconWeight.W500 -> _discoverTuneW500 ?: ImageVector.Builder(
          name = "discover_tune",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(14f, 9.14f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(12.86f, 8f)
            reflectiveQuadTo(13.19f, 7.19f)
            reflectiveQuadTo(14f, 6.86f)
            horizontalLineToRelative(1.86f)
            verticalLineTo(4f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(17f, 2.86f)
            reflectiveQuadToRelative(0.81f, 0.33f)
            reflectiveQuadTo(18.14f, 4f)
            verticalLineTo(6.86f)
            horizontalLineTo(20f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(21.14f, 8f)
            reflectiveQuadTo(20.81f, 8.81f)
            reflectiveQuadTo(20f, 9.14f)
            horizontalLineTo(14f)
            close()
            moveToRelative(2.19f, 11.67f)
            quadTo(15.86f, 20.48f, 15.86f, 20f)
            verticalLineTo(12.19f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(17f, 11.05f)
            reflectiveQuadToRelative(0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(17f, 21.14f)
            reflectiveQuadTo(16.19f, 20.81f)
            close()
            moveToRelative(-10f, 0f)
            quadTo(5.86f, 20.48f, 5.86f, 20f)
            verticalLineTo(17.14f)
            horizontalLineTo(4f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(2.86f, 16f)
            reflectiveQuadTo(3.19f, 15.19f)
            reflectiveQuadTo(4f, 14.86f)
            horizontalLineToRelative(6f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(11.14f, 16f)
            reflectiveQuadToRelative(-0.33f, 0.81f)
            reflectiveQuadTo(10f, 17.14f)
            horizontalLineTo(8.14f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(7f, 21.14f)
            reflectiveQuadTo(6.19f, 20.81f)
            close()
            moveToRelative(0f, -8.19f)
            quadTo(5.86f, 12.29f, 5.86f, 11.81f)
            verticalLineTo(4f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(7f, 2.86f)
            reflectiveQuadTo(7.81f, 3.19f)
            reflectiveQuadTo(8.14f, 4f)
            verticalLineToRelative(7.81f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(7f, 12.95f)
            reflectiveQuadTo(6.19f, 12.62f)
            close()
          }
        }
        .build().also { _discoverTuneW500 = it }
    }

private var _discoverTuneW300: ImageVector? = null
private var _discoverTuneW400: ImageVector? = null
private var _discoverTuneW500: ImageVector? = null

public val FujiIcons.Edit: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _editW300 ?: ImageVector.Builder(
          name = "edit",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.1f, 18.9f)
            horizontalLineTo(6.28f)
            lineTo(16.54f, 8.65f)
            lineTo(15.35f, 7.46f)
            lineTo(5.1f, 17.73f)
            verticalLineTo(18.9f)
            close()
            moveTo(4.56f, 20.3f)
            quadTo(4.2f, 20.3f, 3.95f, 20.05f)
            reflectiveQuadTo(3.7f, 19.44f)
            verticalLineToRelative(-1.6f)
            quadToRelative(0f, -0.34f, 0.13f, -0.66f)
            reflectiveQuadTo(4.21f, 16.63f)
            lineTo(16.73f, 4.1f)
            quadTo(16.94f, 3.91f, 17.19f, 3.81f)
            reflectiveQuadTo(17.72f, 3.7f)
            reflectiveQuadToRelative(0.54f, 0.1f)
            reflectiveQuadToRelative(0.48f, 0.31f)
            lineTo(19.9f, 5.28f)
            quadToRelative(0.21f, 0.2f, 0.3f, 0.46f)
            reflectiveQuadToRelative(0.1f, 0.52f)
            quadToRelative(0f, 0.29f, -0.1f, 0.54f)
            reflectiveQuadTo(19.9f, 7.27f)
            lineTo(7.37f, 19.79f)
            quadTo(7.13f, 20.03f, 6.82f, 20.16f)
            reflectiveQuadTo(6.16f, 20.3f)
            horizontalLineTo(4.56f)
            close()
            moveTo(18.91f, 6.27f)
            lineTo(17.73f, 5.09f)
            lineToRelative(1.18f, 1.18f)
            close()
            moveTo(15.94f, 8.06f)
            lineTo(15.35f, 7.46f)
            lineToRelative(1.19f, 1.19f)
            lineTo(15.94f, 8.06f)
            close()
          }
        }
        .build().also { _editW300 = it }
        IconWeight.W400 -> _editW400 ?: ImageVector.Builder(
          name = "edit",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5f, 19f)
            horizontalLineTo(6.43f)
            lineTo(16.2f, 9.23f)
            lineTo(14.78f, 7.8f)
            lineTo(5f, 17.58f)
            verticalLineTo(19f)
            close()
            moveTo(4f, 21f)
            quadTo(3.58f, 21f, 3.29f, 20.71f)
            quadTo(3f, 20.43f, 3f, 20f)
            verticalLineTo(17.58f)
            quadToRelative(0f, -0.4f, 0.15f, -0.76f)
            reflectiveQuadTo(3.58f, 16.18f)
            lineTo(16.2f, 3.57f)
            quadTo(16.5f, 3.3f, 16.86f, 3.15f)
            reflectiveQuadTo(17.63f, 3f)
            quadToRelative(0.4f, 0f, 0.78f, 0.15f)
            reflectiveQuadTo(19.05f, 3.6f)
            lineTo(20.43f, 5f)
            quadToRelative(0.3f, 0.27f, 0.44f, 0.65f)
            reflectiveQuadTo(21f, 6.4f)
            quadToRelative(0f, 0.4f, -0.14f, 0.76f)
            reflectiveQuadTo(20.43f, 7.82f)
            lineTo(7.83f, 20.43f)
            quadTo(7.55f, 20.7f, 7.19f, 20.85f)
            quadTo(6.83f, 21f, 6.43f, 21f)
            horizontalLineTo(4f)
            close()
            moveTo(19f, 6.4f)
            lineTo(17.6f, 5f)
            lineTo(19f, 6.4f)
            close()
            moveTo(15.48f, 8.52f)
            lineTo(14.78f, 7.8f)
            lineTo(16.2f, 9.23f)
            lineTo(15.48f, 8.52f)
            close()
          }
        }
        .build().also { _editW400 = it }
        IconWeight.W500 -> _editW500 ?: ImageVector.Builder(
          name = "edit",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.07f, 18.93f)
            horizontalLineTo(6.5f)
            lineTo(15.87f, 9.56f)
            lineTo(14.45f, 8.13f)
            lineTo(5.07f, 17.52f)
            verticalLineToRelative(1.41f)
            close()
            moveTo(3.93f, 21.2f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(2.8f, 20.07f)
            verticalLineTo(17.51f)
            quadToRelative(0f, -0.45f, 0.17f, -0.87f)
            reflectiveQuadTo(3.46f, 15.91f)
            lineTo(15.98f, 3.4f)
            quadTo(16.3f, 3.11f, 16.68f, 2.95f)
            reflectiveQuadTo(17.48f, 2.8f)
            reflectiveQuadToRelative(0.8f, 0.16f)
            reflectiveQuadToRelative(0.69f, 0.47f)
            lineTo(20.6f, 5.06f)
            quadToRelative(0.31f, 0.29f, 0.46f, 0.69f)
            reflectiveQuadToRelative(0.15f, 0.79f)
            quadToRelative(0f, 0.42f, -0.15f, 0.8f)
            reflectiveQuadTo(20.6f, 8.04f)
            lineTo(8.1f, 20.54f)
            quadTo(7.78f, 20.86f, 7.37f, 21.03f)
            reflectiveQuadTo(6.5f, 21.2f)
            horizontalLineTo(3.93f)
            close()
            moveTo(18.87f, 6.54f)
            lineTo(17.46f, 5.13f)
            lineToRelative(1.41f, 1.41f)
            close()
            moveTo(15.15f, 8.85f)
            lineTo(14.45f, 8.13f)
            lineToRelative(1.42f, 1.42f)
            lineTo(15.15f, 8.85f)
            close()
          }
        }
        .build().also { _editW500 = it }
    }

private var _editW300: ImageVector? = null
private var _editW400: ImageVector? = null
private var _editW500: ImageVector? = null

public val FujiIcons.Exposure: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _exposureW300 ?: ImageVector.Builder(
          name = "exposure",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.39f, 20.32f)
            quadToRelative(-0.71f, 0f, -1.21f, -0.5f)
            reflectiveQuadTo(3.68f, 18.61f)
            verticalLineTo(5.39f)
            quadToRelative(0f, -0.71f, 0.5f, -1.21f)
            reflectiveQuadTo(5.39f, 3.68f)
            horizontalLineTo(18.61f)
            quadToRelative(0.71f, 0f, 1.21f, 0.5f)
            reflectiveQuadToRelative(0.5f, 1.21f)
            verticalLineTo(18.61f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(5.39f)
            close()
            moveToRelative(0f, -1.4f)
            horizontalLineTo(18.61f)
            quadToRelative(0.12f, 0f, 0.21f, -0.1f)
            reflectiveQuadToRelative(0.1f, -0.21f)
            verticalLineTo(5.39f)
            quadToRelative(0f, -0.06f, -0.02f, -0.11f)
            reflectiveQuadTo(18.82f, 5.18f)
            lineTo(5.18f, 18.82f)
            quadToRelative(0.05f, 0.05f, 0.1f, 0.07f)
            reflectiveQuadToRelative(0.11f, 0.02f)
            close()
            moveToRelative(9.27f, -3.15f)
            horizontalLineTo(13.23f)
            quadToRelative(-0.24f, 0f, -0.4f, -0.16f)
            reflectiveQuadToRelative(-0.16f, -0.4f)
            reflectiveQuadToRelative(0.16f, -0.4f)
            reflectiveQuadToRelative(0.4f, -0.16f)
            horizontalLineToRelative(1.42f)
            verticalLineTo(13.23f)
            quadToRelative(0f, -0.24f, 0.16f, -0.4f)
            reflectiveQuadToRelative(0.4f, -0.16f)
            reflectiveQuadToRelative(0.4f, 0.16f)
            reflectiveQuadToRelative(0.16f, 0.4f)
            verticalLineToRelative(1.42f)
            horizontalLineToRelative(1.42f)
            quadToRelative(0.24f, 0f, 0.4f, 0.16f)
            reflectiveQuadToRelative(0.16f, 0.4f)
            reflectiveQuadToRelative(-0.16f, 0.4f)
            reflectiveQuadToRelative(-0.4f, 0.16f)
            horizontalLineTo(15.77f)
            verticalLineToRelative(1.42f)
            quadToRelative(0f, 0.24f, -0.16f, 0.4f)
            reflectiveQuadToRelative(-0.4f, 0.16f)
            reflectiveQuadToRelative(-0.4f, -0.16f)
            reflectiveQuadToRelative(-0.16f, -0.4f)
            verticalLineTo(15.77f)
            close()
            moveTo(10.27f, 8.35f)
            quadToRelative(0.24f, 0f, 0.4f, -0.16f)
            reflectiveQuadToRelative(0.16f, -0.4f)
            reflectiveQuadTo(10.67f, 7.39f)
            reflectiveQuadTo(10.27f, 7.23f)
            horizontalLineTo(6.81f)
            quadToRelative(-0.24f, 0f, -0.4f, 0.16f)
            reflectiveQuadTo(6.25f, 7.79f)
            reflectiveQuadToRelative(0.16f, 0.4f)
            reflectiveQuadToRelative(0.4f, 0.16f)
            horizontalLineToRelative(3.46f)
            close()
          }
        }
        .build().also { _exposureW300 = it }
        IconWeight.W400 -> _exposureW400 ?: ImageVector.Builder(
          name = "exposure",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(19f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 19f)
            horizontalLineTo(19f)
            verticalLineTo(5f)
            lineTo(5f, 19f)
            close()
            moveToRelative(9.5f, -3f)
            horizontalLineTo(13.25f)
            quadToRelative(-0.32f, 0f, -0.54f, -0.21f)
            reflectiveQuadTo(12.5f, 15.25f)
            reflectiveQuadToRelative(0.21f, -0.54f)
            reflectiveQuadTo(13.25f, 14.5f)
            horizontalLineTo(14.5f)
            verticalLineTo(13.25f)
            quadToRelative(0f, -0.33f, 0.21f, -0.54f)
            reflectiveQuadTo(15.25f, 12.5f)
            reflectiveQuadToRelative(0.54f, 0.21f)
            quadTo(16f, 12.93f, 16f, 13.25f)
            verticalLineTo(14.5f)
            horizontalLineToRelative(1.25f)
            quadToRelative(0.32f, 0f, 0.54f, 0.21f)
            reflectiveQuadTo(18f, 15.25f)
            reflectiveQuadToRelative(-0.21f, 0.54f)
            reflectiveQuadTo(17.25f, 16f)
            horizontalLineTo(16f)
            verticalLineToRelative(1.25f)
            quadToRelative(0f, 0.32f, -0.21f, 0.54f)
            reflectiveQuadTo(15.25f, 18f)
            reflectiveQuadTo(14.71f, 17.79f)
            reflectiveQuadTo(14.5f, 17.25f)
            verticalLineTo(16f)
            close()
            moveTo(10.25f, 8.5f)
            quadToRelative(0.33f, 0f, 0.54f, -0.21f)
            reflectiveQuadTo(11f, 7.75f)
            reflectiveQuadTo(10.79f, 7.21f)
            reflectiveQuadTo(10.25f, 7f)
            horizontalLineTo(6.75f)
            quadTo(6.43f, 7f, 6.21f, 7.21f)
            quadTo(6f, 7.43f, 6f, 7.75f)
            reflectiveQuadTo(6.21f, 8.29f)
            reflectiveQuadTo(6.75f, 8.5f)
            horizontalLineToRelative(3.5f)
            close()
          }
        }
        .build().also { _exposureW400 = it }
        IconWeight.W500 -> _exposureW500 ?: ImageVector.Builder(
          name = "exposure",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.07f, 21.2f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(2.8f, 18.93f)
            verticalLineTo(5.07f)
            quadTo(2.8f, 4.13f, 3.46f, 3.46f)
            reflectiveQuadTo(5.07f, 2.8f)
            horizontalLineTo(18.93f)
            quadToRelative(0.94f, 0f, 1.61f, 0.67f)
            reflectiveQuadTo(21.2f, 5.07f)
            verticalLineTo(18.93f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(18.93f, 21.2f)
            horizontalLineTo(5.07f)
            close()
            moveToRelative(0f, -2.28f)
            horizontalLineTo(18.93f)
            verticalLineTo(5.07f)
            lineTo(5.07f, 18.93f)
            close()
            moveToRelative(9.28f, -2.98f)
            horizontalLineToRelative(-1.2f)
            quadToRelative(-0.35f, 0f, -0.57f, -0.23f)
            reflectiveQuadTo(12.35f, 15.15f)
            reflectiveQuadToRelative(0.23f, -0.57f)
            reflectiveQuadToRelative(0.57f, -0.23f)
            horizontalLineToRelative(1.2f)
            verticalLineToRelative(-1.2f)
            quadToRelative(0f, -0.35f, 0.23f, -0.57f)
            reflectiveQuadToRelative(0.57f, -0.23f)
            reflectiveQuadToRelative(0.57f, 0.23f)
            reflectiveQuadToRelative(0.23f, 0.57f)
            verticalLineToRelative(1.2f)
            horizontalLineToRelative(1.2f)
            quadToRelative(0.35f, 0f, 0.57f, 0.23f)
            reflectiveQuadToRelative(0.23f, 0.57f)
            reflectiveQuadToRelative(-0.23f, 0.57f)
            reflectiveQuadToRelative(-0.57f, 0.23f)
            horizontalLineToRelative(-1.2f)
            verticalLineToRelative(1.2f)
            quadToRelative(0f, 0.35f, -0.23f, 0.57f)
            reflectiveQuadToRelative(-0.57f, 0.23f)
            reflectiveQuadTo(14.58f, 17.72f)
            reflectiveQuadTo(14.35f, 17.15f)
            verticalLineToRelative(-1.2f)
            close()
            moveToRelative(-4.1f, -7.4f)
            quadToRelative(0.35f, 0f, 0.57f, -0.23f)
            reflectiveQuadTo(11.05f, 7.75f)
            reflectiveQuadTo(10.82f, 7.18f)
            reflectiveQuadTo(10.25f, 6.95f)
            horizontalLineTo(6.75f)
            quadTo(6.4f, 6.95f, 6.18f, 7.18f)
            reflectiveQuadTo(5.95f, 7.75f)
            reflectiveQuadTo(6.18f, 8.32f)
            reflectiveQuadTo(6.75f, 8.55f)
            horizontalLineToRelative(3.5f)
            close()
          }
        }
        .build().also { _exposureW500 = it }
    }

private var _exposureW300: ImageVector? = null
private var _exposureW400: ImageVector? = null
private var _exposureW500: ImageVector? = null

public val FujiIcons.FileExport: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _fileExportW300 ?: ImageVector.Builder(
          name = "file_export",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 12f)
            close()
            moveTo(8.61f, 18.7f)
            lineTo(5.85f, 21.45f)
            quadToRelative(-0.22f, 0.21f, -0.5f, 0.21f)
            reflectiveQuadTo(4.86f, 21.44f)
            quadTo(4.66f, 21.23f, 4.66f, 20.95f)
            reflectiveQuadToRelative(0.21f, -0.5f)
            lineTo(7.63f, 17.7f)
            horizontalLineTo(5.68f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.2f)
            reflectiveQuadTo(4.99f, 17f)
            reflectiveQuadToRelative(0.2f, -0.5f)
            reflectiveQuadTo(5.68f, 16.3f)
            horizontalLineTo(9.15f)
            quadToRelative(0.36f, 0f, 0.61f, 0.25f)
            reflectiveQuadTo(10f, 17.15f)
            verticalLineToRelative(3.47f)
            quadToRelative(0f, 0.29f, -0.21f, 0.49f)
            reflectiveQuadToRelative(-0.5f, 0.2f)
            reflectiveQuadTo(8.81f, 21.11f)
            reflectiveQuadTo(8.61f, 20.62f)
            verticalLineTo(18.7f)
            close()
            moveTo(4.89f, 14f)
            quadTo(4.68f, 13.8f, 4.68f, 13.51f)
            verticalLineTo(4.41f)
            quadTo(4.68f, 3.7f, 5.18f, 3.2f)
            reflectiveQuadTo(6.39f, 2.7f)
            horizontalLineToRelative(7.88f)
            lineToRelative(5.05f, 5.05f)
            verticalLineTo(19.59f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(12.79f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.21f)
            reflectiveQuadTo(12.1f, 20.6f)
            reflectiveQuadToRelative(0.2f, -0.49f)
            reflectiveQuadTo(12.79f, 19.9f)
            horizontalLineToRelative(4.82f)
            quadToRelative(0.13f, 0f, 0.22f, -0.09f)
            reflectiveQuadToRelative(0.09f, -0.22f)
            verticalLineTo(8.46f)
            horizontalLineTo(14.41f)
            quadToRelative(-0.35f, 0f, -0.6f, -0.25f)
            reflectiveQuadTo(13.56f, 7.61f)
            verticalLineTo(4.1f)
            horizontalLineTo(6.39f)
            quadTo(6.25f, 4.1f, 6.17f, 4.19f)
            reflectiveQuadTo(6.08f, 4.41f)
            verticalLineToRelative(9.1f)
            quadTo(6.08f, 13.8f, 5.87f, 14f)
            reflectiveQuadToRelative(-0.49f, 0.2f)
            reflectiveQuadTo(4.89f, 14f)
            close()
          }
        }
        .build().also { _fileExportW300 = it }
        IconWeight.W400 -> _fileExportW400 ?: ImageVector.Builder(
          name = "file_export",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 12f)
            close()
            moveTo(8f, 19.43f)
            lineTo(5.75f, 21.68f)
            quadToRelative(-0.3f, 0.3f, -0.7f, 0.29f)
            reflectiveQuadTo(4.35f, 21.65f)
            quadTo(4.08f, 21.35f, 4.06f, 20.95f)
            reflectiveQuadToRelative(0.29f, -0.7f)
            lineTo(6.6f, 18f)
            horizontalLineTo(5.35f)
            quadTo(4.93f, 18f, 4.64f, 17.71f)
            quadTo(4.35f, 17.43f, 4.35f, 17f)
            reflectiveQuadTo(4.64f, 16.29f)
            reflectiveQuadTo(5.35f, 16f)
            horizontalLineTo(9f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(10f, 17f)
            verticalLineToRelative(3.65f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(9f, 21.65f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            reflectiveQuadTo(8f, 20.65f)
            verticalLineTo(19.43f)
            close()
            moveTo(4.29f, 13.71f)
            quadTo(4f, 13.43f, 4f, 13f)
            verticalLineTo(4f)
            quadTo(4f, 3.17f, 4.59f, 2.59f)
            reflectiveQuadTo(6f, 2f)
            horizontalLineToRelative(8f)
            lineToRelative(6f, 6f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(18f, 22f)
            horizontalLineTo(13f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(12f, 21.43f, 12f, 21f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(13f, 20f)
            horizontalLineToRelative(5f)
            verticalLineTo(9f)
            horizontalLineTo(14f)
            quadTo(13.58f, 9f, 13.29f, 8.71f)
            reflectiveQuadTo(13f, 8f)
            verticalLineTo(4f)
            horizontalLineTo(6f)
            verticalLineToRelative(9f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(5f, 14f)
            quadTo(4.58f, 14f, 4.29f, 13.71f)
            close()
          }
        }
        .build().also { _fileExportW400 = it }
        IconWeight.W500 -> _fileExportW500 ?: ImageVector.Builder(
          name = "file_export",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 12f)
            close()
            moveTo(7.89f, 19.75f)
            lineTo(5.88f, 21.77f)
            quadTo(5.53f, 22.11f, 5.07f, 22.1f)
            reflectiveQuadTo(4.28f, 21.75f)
            quadTo(3.96f, 21.4f, 3.95f, 20.95f)
            reflectiveQuadTo(4.28f, 20.15f)
            lineTo(6.3f, 18.14f)
            horizontalLineTo(5.25f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(4.11f, 17f)
            reflectiveQuadTo(4.44f, 16.19f)
            reflectiveQuadTo(5.25f, 15.86f)
            horizontalLineTo(9.02f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(10.16f, 17f)
            verticalLineToRelative(3.78f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(9.02f, 21.91f)
            reflectiveQuadTo(8.22f, 21.58f)
            reflectiveQuadTo(7.89f, 20.78f)
            verticalLineTo(19.75f)
            close()
            moveTo(4.13f, 13.53f)
            quadTo(3.8f, 13.2f, 3.8f, 12.72f)
            verticalLineTo(4.07f)
            quadTo(3.8f, 3.13f, 4.46f, 2.46f)
            reflectiveQuadTo(6.07f, 1.8f)
            horizontalLineToRelative(8.01f)
            lineTo(20.2f, 7.92f)
            verticalLineTo(19.93f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(17.93f, 22.2f)
            horizontalLineTo(13.3f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(12.16f, 21.07f)
            reflectiveQuadToRelative(0.33f, -0.81f)
            reflectiveQuadTo(13.3f, 19.93f)
            horizontalLineToRelative(4.63f)
            verticalLineTo(9.07f)
            horizontalLineTo(14.07f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(12.93f, 7.93f)
            verticalLineTo(4.07f)
            horizontalLineTo(6.07f)
            verticalLineToRelative(8.65f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(4.93f, 13.86f)
            reflectiveQuadTo(4.13f, 13.53f)
            close()
          }
        }
        .build().also { _fileExportW500 = it }
    }

private var _fileExportW300: ImageVector? = null
private var _fileExportW400: ImageVector? = null
private var _fileExportW500: ImageVector? = null

public val FujiIcons.FileSave: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _fileSaveW300 ?: ImageVector.Builder(
          name = "file_save",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(17.3f, 17.93f)
            lineTo(15.88f, 16.5f)
            quadToRelative(-0.2f, -0.2f, -0.47f, -0.19f)
            reflectiveQuadToRelative(-0.48f, 0.21f)
            reflectiveQuadTo(14.72f, 17f)
            reflectiveQuadToRelative(0.2f, 0.48f)
            lineToRelative(2.48f, 2.49f)
            quadToRelative(0.25f, 0.26f, 0.6f, 0.26f)
            reflectiveQuadTo(18.6f, 19.97f)
            lineTo(21.1f, 17.48f)
            quadTo(21.29f, 17.28f, 21.3f, 17f)
            reflectiveQuadTo(21.1f, 16.5f)
            reflectiveQuadTo(20.61f, 16.3f)
            reflectiveQuadToRelative(-0.48f, 0.2f)
            lineTo(18.7f, 17.93f)
            verticalLineToRelative(-3.9f)
            quadToRelative(0f, -0.29f, -0.21f, -0.5f)
            reflectiveQuadTo(18f, 13.32f)
            reflectiveQuadToRelative(-0.49f, 0.21f)
            reflectiveQuadToRelative(-0.2f, 0.5f)
            verticalLineToRelative(3.9f)
            close()
            moveTo(15f, 22.3f)
            horizontalLineToRelative(6f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadTo(21.7f, 23f)
            reflectiveQuadTo(21.5f, 23.49f)
            reflectiveQuadTo(21f, 23.7f)
            horizontalLineTo(15f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.2f)
            reflectiveQuadTo(14.3f, 23f)
            reflectiveQuadToRelative(0.2f, -0.5f)
            reflectiveQuadTo(15f, 22.3f)
            close()
            moveTo(6.01f, 19.7f)
            quadToRelative(-0.7f, 0f, -1.2f, -0.51f)
            reflectiveQuadTo(4.3f, 17.99f)
            verticalLineTo(4f)
            quadToRelative(0f, -0.69f, 0.51f, -1.2f)
            reflectiveQuadTo(6.01f, 2.3f)
            horizontalLineToRelative(6.26f)
            quadToRelative(0.35f, 0f, 0.66f, 0.14f)
            reflectiveQuadTo(13.49f, 2.8f)
            lineTo(18.2f, 7.51f)
            quadToRelative(0.23f, 0.24f, 0.37f, 0.55f)
            reflectiveQuadTo(18.7f, 8.73f)
            verticalLineToRelative(1.81f)
            quadToRelative(0f, 0.29f, -0.21f, 0.49f)
            reflectiveQuadTo(18f, 11.23f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.2f)
            reflectiveQuadTo(17.3f, 10.53f)
            verticalLineTo(8.7f)
            horizontalLineTo(13.59f)
            quadToRelative(-0.53f, 0f, -0.91f, -0.37f)
            reflectiveQuadTo(12.3f, 7.41f)
            verticalLineTo(3.7f)
            horizontalLineTo(6.01f)
            quadTo(5.89f, 3.7f, 5.8f, 3.79f)
            reflectiveQuadTo(5.7f, 4f)
            verticalLineTo(17.99f)
            quadToRelative(0f, 0.12f, 0.09f, 0.21f)
            reflectiveQuadTo(6.01f, 18.3f)
            horizontalLineToRelative(5.5f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadTo(12.21f, 19f)
            quadToRelative(0f, 0.29f, -0.21f, 0.49f)
            reflectiveQuadToRelative(-0.5f, 0.2f)
            horizontalLineTo(6.01f)
            close()
            moveTo(5.7f, 18.3f)
            verticalLineTo(12.94f)
            quadToRelative(0f, -0.7f, 0f, -1.21f)
            reflectiveQuadToRelative(0f, -0.5f)
            verticalLineTo(8.7f)
            verticalLineToRelative(-5f)
            quadToRelative(0f, 0f, 0f, 0.1f)
            reflectiveQuadTo(5.7f, 4f)
            verticalLineTo(17.99f)
            quadToRelative(0f, 0.12f, 0f, 0.21f)
            reflectiveQuadToRelative(0f, 0.09f)
            close()
          }
        }
        .build().also { _fileSaveW300 = it }
        IconWeight.W400 -> _fileSaveW400 ?: ImageVector.Builder(
          name = "file_save",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(17f, 17.2f)
            lineTo(16.1f, 16.3f)
            quadTo(15.83f, 16.02f, 15.4f, 16.02f)
            reflectiveQuadTo(14.7f, 16.3f)
            reflectiveQuadTo(14.43f, 17f)
            reflectiveQuadToRelative(0.28f, 0.7f)
            lineToRelative(2.6f, 2.6f)
            quadToRelative(0.3f, 0.3f, 0.7f, 0.3f)
            reflectiveQuadToRelative(0.7f, -0.3f)
            lineToRelative(2.6f, -2.6f)
            quadTo(21.58f, 17.43f, 21.58f, 17f)
            reflectiveQuadTo(21.3f, 16.3f)
            quadTo(21.03f, 16.02f, 20.6f, 16.02f)
            reflectiveQuadTo(19.9f, 16.3f)
            lineTo(19f, 17.2f)
            verticalLineTo(14.02f)
            quadTo(19f, 13.6f, 18.71f, 13.31f)
            quadTo(18.43f, 13.02f, 18f, 13.02f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(17f, 14.02f)
            verticalLineTo(17.2f)
            close()
            moveTo(15f, 22f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 23f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(21f, 24f)
            horizontalLineTo(15f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(14f, 23.43f, 14f, 23f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(15f, 22f)
            close()
            moveTo(6f, 20f)
            quadTo(5.18f, 20f, 4.59f, 19.41f)
            reflectiveQuadTo(4f, 18f)
            verticalLineTo(4f)
            quadTo(4f, 3.17f, 4.59f, 2.59f)
            reflectiveQuadTo(6f, 2f)
            horizontalLineToRelative(6.18f)
            quadToRelative(0.4f, 0f, 0.76f, 0.15f)
            reflectiveQuadToRelative(0.64f, 0.43f)
            lineToRelative(4.85f, 4.85f)
            quadTo(18.7f, 7.7f, 18.85f, 8.06f)
            quadTo(19f, 8.42f, 19f, 8.82f)
            verticalLineToRelative(1.2f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(18f, 11.02f)
            quadToRelative(-0.43f, 0f, -0.71f, -0.29f)
            reflectiveQuadTo(17f, 10.02f)
            verticalLineTo(9f)
            horizontalLineTo(13.5f)
            quadTo(12.88f, 9f, 12.44f, 8.56f)
            reflectiveQuadTo(12f, 7.5f)
            verticalLineTo(4f)
            horizontalLineTo(6f)
            verticalLineTo(18f)
            horizontalLineToRelative(5f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(12f, 19f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(11f, 20f)
            horizontalLineTo(6f)
            close()
            moveTo(6f, 18f)
            verticalLineTo(13.02f)
            quadTo(6f, 12.2f, 6f, 11.61f)
            reflectiveQuadTo(6f, 11.02f)
            verticalLineTo(9f)
            verticalLineTo(4f)
            verticalLineTo(18f)
            close()
          }
        }
        .build().also { _fileSaveW400 = it }
        IconWeight.W500 -> _fileSaveW500 ?: ImageVector.Builder(
          name = "file_save",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(17f, 17.2f)
            lineTo(16.09f, 16.29f)
            quadToRelative(-0.27f, -0.27f, -0.7f, -0.27f)
            reflectiveQuadToRelative(-0.7f, 0.27f)
            reflectiveQuadToRelative(-0.28f, 0.7f)
            reflectiveQuadToRelative(0.28f, 0.7f)
            lineTo(17.2f, 20.2f)
            quadToRelative(0.34f, 0.34f, 0.8f, 0.34f)
            reflectiveQuadTo(18.8f, 20.2f)
            lineToRelative(2.51f, -2.51f)
            quadToRelative(0.27f, -0.28f, 0.27f, -0.7f)
            reflectiveQuadToRelative(-0.27f, -0.7f)
            reflectiveQuadToRelative(-0.7f, -0.27f)
            reflectiveQuadToRelative(-0.7f, 0.27f)
            lineTo(19f, 17.2f)
            verticalLineTo(14.02f)
            quadTo(19f, 13.6f, 18.71f, 13.31f)
            quadTo(18.43f, 13.02f, 18f, 13.02f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(17f, 14.02f)
            verticalLineTo(17.2f)
            close()
            moveTo(15f, 22f)
            horizontalLineToRelative(6f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 23f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(21f, 24f)
            horizontalLineTo(15f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(14f, 23.43f, 14f, 23f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(15f, 22f)
            close()
            moveTo(6.14f, 20.14f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(3.86f, 17.86f)
            verticalLineTo(4.14f)
            quadToRelative(0f, -0.94f, 0.67f, -1.61f)
            reflectiveQuadTo(6.14f, 1.86f)
            horizontalLineToRelative(6.07f)
            quadToRelative(0.46f, 0f, 0.87f, 0.17f)
            reflectiveQuadToRelative(0.73f, 0.49f)
            lineToRelative(4.67f, 4.67f)
            quadToRelative(0.32f, 0.32f, 0.49f, 0.73f)
            reflectiveQuadToRelative(0.17f, 0.87f)
            verticalLineToRelative(1.1f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(18f, 11.02f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(16.86f, 9.89f)
            verticalLineTo(9.05f)
            horizontalLineToRelative(-3.2f)
            quadToRelative(-0.71f, 0f, -1.21f, -0.5f)
            reflectiveQuadTo(11.95f, 7.34f)
            verticalLineTo(4.14f)
            horizontalLineTo(6.14f)
            verticalLineTo(17.86f)
            horizontalLineToRelative(4.72f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(12f, 19f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadToRelative(-0.81f, 0.33f)
            horizontalLineTo(6.14f)
            close()
            moveToRelative(0f, -2.28f)
            verticalLineTo(13.3f)
            quadToRelative(0f, -0.94f, 0f, -1.61f)
            reflectiveQuadToRelative(0f, -0.67f)
            verticalLineTo(9.05f)
            verticalLineTo(4.14f)
            verticalLineTo(17.86f)
            close()
          }
        }
        .build().also { _fileSaveW500 = it }
    }

private var _fileSaveW300: ImageVector? = null
private var _fileSaveW400: ImageVector? = null
private var _fileSaveW500: ImageVector? = null

public val FujiIcons.Grain: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _grainW300 ?: ImageVector.Builder(
          name = "grain",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6f, 19.77f)
            quadToRelative(-0.75f, 0f, -1.26f, -0.51f)
            reflectiveQuadTo(4.23f, 18f)
            reflectiveQuadTo(4.74f, 16.74f)
            reflectiveQuadTo(6f, 16.23f)
            reflectiveQuadToRelative(1.26f, 0.51f)
            reflectiveQuadTo(7.77f, 18f)
            reflectiveQuadTo(7.26f, 19.26f)
            reflectiveQuadTo(6f, 19.77f)
            close()
            moveToRelative(8f, 0f)
            quadToRelative(-0.75f, 0f, -1.26f, -0.51f)
            reflectiveQuadTo(12.23f, 18f)
            reflectiveQuadToRelative(0.51f, -1.26f)
            reflectiveQuadTo(14f, 16.23f)
            reflectiveQuadToRelative(1.26f, 0.51f)
            reflectiveQuadTo(15.77f, 18f)
            reflectiveQuadToRelative(-0.51f, 1.26f)
            reflectiveQuadTo(14f, 19.77f)
            close()
            moveToRelative(-4f, -4f)
            quadToRelative(-0.75f, 0f, -1.26f, -0.51f)
            reflectiveQuadTo(8.23f, 14f)
            reflectiveQuadTo(8.74f, 12.74f)
            reflectiveQuadTo(10f, 12.23f)
            reflectiveQuadToRelative(1.26f, 0.51f)
            reflectiveQuadTo(11.77f, 14f)
            reflectiveQuadToRelative(-0.51f, 1.26f)
            reflectiveQuadTo(10f, 15.77f)
            close()
            moveToRelative(8f, 0f)
            quadToRelative(-0.75f, 0f, -1.26f, -0.51f)
            reflectiveQuadTo(16.23f, 14f)
            reflectiveQuadToRelative(0.51f, -1.26f)
            reflectiveQuadTo(18f, 12.23f)
            reflectiveQuadToRelative(1.26f, 0.51f)
            reflectiveQuadTo(19.77f, 14f)
            reflectiveQuadToRelative(-0.51f, 1.26f)
            reflectiveQuadTo(18f, 15.77f)
            close()
            moveToRelative(-12f, -4f)
            quadToRelative(-0.75f, 0f, -1.26f, -0.51f)
            reflectiveQuadTo(4.23f, 10f)
            reflectiveQuadTo(4.74f, 8.74f)
            reflectiveQuadTo(6f, 8.23f)
            reflectiveQuadTo(7.26f, 8.74f)
            reflectiveQuadTo(7.77f, 10f)
            reflectiveQuadTo(7.26f, 11.26f)
            reflectiveQuadTo(6f, 11.77f)
            close()
            moveToRelative(8f, 0f)
            quadToRelative(-0.75f, 0f, -1.26f, -0.51f)
            reflectiveQuadTo(12.23f, 10f)
            reflectiveQuadTo(12.74f, 8.74f)
            reflectiveQuadTo(14f, 8.23f)
            reflectiveQuadToRelative(1.26f, 0.51f)
            reflectiveQuadTo(15.77f, 10f)
            reflectiveQuadToRelative(-0.51f, 1.26f)
            reflectiveQuadTo(14f, 11.77f)
            close()
            moveToRelative(-4f, -4f)
            quadTo(9.25f, 7.77f, 8.74f, 7.26f)
            reflectiveQuadTo(8.23f, 6f)
            reflectiveQuadTo(8.74f, 4.74f)
            reflectiveQuadTo(10f, 4.23f)
            reflectiveQuadToRelative(1.26f, 0.51f)
            reflectiveQuadTo(11.77f, 6f)
            reflectiveQuadTo(11.26f, 7.26f)
            reflectiveQuadTo(10f, 7.77f)
            close()
            moveToRelative(8f, 0f)
            quadToRelative(-0.75f, 0f, -1.26f, -0.51f)
            reflectiveQuadTo(16.23f, 6f)
            reflectiveQuadTo(16.74f, 4.74f)
            reflectiveQuadTo(18f, 4.23f)
            reflectiveQuadToRelative(1.26f, 0.51f)
            reflectiveQuadTo(19.77f, 6f)
            reflectiveQuadTo(19.26f, 7.26f)
            reflectiveQuadTo(18f, 7.77f)
            close()
          }
        }
        .build().also { _grainW300 = it }
        IconWeight.W400 -> _grainW400 ?: ImageVector.Builder(
          name = "grain",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6f, 20f)
            quadTo(5.18f, 20f, 4.59f, 19.41f)
            reflectiveQuadTo(4f, 18f)
            reflectiveQuadTo(4.59f, 16.59f)
            reflectiveQuadTo(6f, 16f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(8f, 17.18f, 8f, 18f)
            reflectiveQuadTo(7.41f, 19.41f)
            reflectiveQuadTo(6f, 20f)
            close()
            moveToRelative(8f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(14f, 16f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(16f, 17.18f, 16f, 18f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(14f, 20f)
            close()
            moveTo(10f, 16f)
            quadTo(9.18f, 16f, 8.59f, 15.41f)
            reflectiveQuadTo(8f, 14f)
            reflectiveQuadTo(8.59f, 12.59f)
            reflectiveQuadTo(10f, 12f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(12f, 13.18f, 12f, 14f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(10f, 16f)
            close()
            moveToRelative(8f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(16f, 14f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(18f, 12f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(20f, 13.18f, 20f, 14f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(18f, 16f)
            close()
            moveTo(6f, 12f)
            quadTo(5.18f, 12f, 4.59f, 11.41f)
            reflectiveQuadTo(4f, 10f)
            quadTo(4f, 9.17f, 4.59f, 8.59f)
            reflectiveQuadTo(6f, 8f)
            quadTo(6.83f, 8f, 7.41f, 8.59f)
            reflectiveQuadTo(8f, 10f)
            reflectiveQuadTo(7.41f, 11.41f)
            reflectiveQuadTo(6f, 12f)
            close()
            moveToRelative(8f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(12f, 10f)
            quadTo(12f, 9.17f, 12.59f, 8.59f)
            reflectiveQuadTo(14f, 8f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            reflectiveQuadTo(16f, 10f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(14f, 12f)
            close()
            moveTo(10f, 8f)
            quadTo(9.18f, 8f, 8.59f, 7.41f)
            reflectiveQuadTo(8f, 6f)
            reflectiveQuadTo(8.59f, 4.59f)
            reflectiveQuadTo(10f, 4f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(12f, 5.18f, 12f, 6f)
            reflectiveQuadTo(11.41f, 7.41f)
            reflectiveQuadTo(10f, 8f)
            close()
            moveToRelative(8f, 0f)
            quadTo(17.18f, 8f, 16.59f, 7.41f)
            reflectiveQuadTo(16f, 6f)
            reflectiveQuadTo(16.59f, 4.59f)
            reflectiveQuadTo(18f, 4f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(20f, 5.18f, 20f, 6f)
            reflectiveQuadTo(19.41f, 7.41f)
            reflectiveQuadTo(18f, 8f)
            close()
          }
        }
        .build().also { _grainW400 = it }
        IconWeight.W500 -> _grainW500 ?: ImageVector.Builder(
          name = "grain",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.77f, 20.28f)
            quadToRelative(-0.87f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(3.68f, 18.19f)
            reflectiveQuadTo(4.29f, 16.7f)
            reflectiveQuadTo(5.77f, 16.09f)
            reflectiveQuadTo(7.25f, 16.7f)
            reflectiveQuadToRelative(0.61f, 1.48f)
            reflectiveQuadTo(7.25f, 19.66f)
            reflectiveQuadTo(5.77f, 20.28f)
            close()
            moveToRelative(8.36f, 0f)
            quadToRelative(-0.87f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(12.05f, 18.19f)
            reflectiveQuadTo(12.66f, 16.7f)
            reflectiveQuadToRelative(1.48f, -0.61f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadToRelative(0.61f, 1.48f)
            reflectiveQuadToRelative(-0.61f, 1.48f)
            reflectiveQuadToRelative(-1.48f, 0.61f)
            close()
            moveTo(9.95f, 16.19f)
            quadToRelative(-0.87f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(7.86f, 14.09f)
            reflectiveQuadTo(8.47f, 12.61f)
            reflectiveQuadTo(9.95f, 12f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadToRelative(0.61f, 1.48f)
            reflectiveQuadToRelative(-0.61f, 1.48f)
            reflectiveQuadTo(9.95f, 16.19f)
            close()
            moveToRelative(8.28f, 0f)
            quadToRelative(-0.87f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(16.14f, 14.09f)
            reflectiveQuadToRelative(0.61f, -1.48f)
            reflectiveQuadTo(18.23f, 12f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadToRelative(0.61f, 1.48f)
            reflectiveQuadToRelative(-0.61f, 1.48f)
            reflectiveQuadToRelative(-1.48f, 0.61f)
            close()
            moveTo(5.77f, 12f)
            quadTo(4.91f, 12f, 4.29f, 11.39f)
            reflectiveQuadTo(3.68f, 9.91f)
            reflectiveQuadTo(4.29f, 8.43f)
            reflectiveQuadTo(5.77f, 7.81f)
            reflectiveQuadTo(7.25f, 8.43f)
            reflectiveQuadTo(7.86f, 9.91f)
            reflectiveQuadTo(7.25f, 11.39f)
            reflectiveQuadTo(5.77f, 12f)
            close()
            moveToRelative(8.36f, 0f)
            quadToRelative(-0.87f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(12.05f, 9.91f)
            reflectiveQuadTo(12.66f, 8.43f)
            reflectiveQuadTo(14.14f, 7.81f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadToRelative(0.61f, 1.48f)
            reflectiveQuadToRelative(-0.61f, 1.48f)
            reflectiveQuadTo(14.14f, 12f)
            close()
            moveTo(9.95f, 7.91f)
            quadTo(9.09f, 7.91f, 8.47f, 7.3f)
            reflectiveQuadTo(7.86f, 5.81f)
            reflectiveQuadTo(8.47f, 4.34f)
            reflectiveQuadTo(9.95f, 3.72f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadToRelative(0.61f, 1.48f)
            reflectiveQuadTo(11.43f, 7.3f)
            reflectiveQuadTo(9.95f, 7.91f)
            close()
            moveToRelative(8.28f, 0f)
            quadToRelative(-0.87f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(16.14f, 5.81f)
            reflectiveQuadTo(16.75f, 4.34f)
            reflectiveQuadTo(18.23f, 3.72f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadToRelative(0.61f, 1.48f)
            reflectiveQuadTo(19.71f, 7.3f)
            reflectiveQuadTo(18.23f, 7.91f)
            close()
          }
        }
        .build().also { _grainW500 = it }
    }

private var _grainW300: ImageVector? = null
private var _grainW400: ImageVector? = null
private var _grainW500: ImageVector? = null

public val FujiIcons.ImageSearch: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _imageSearchW300 ?: ImageVector.Builder(
          name = "image_search",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.23f, 16f)
            lineTo(9.79f, 14.16f)
            quadTo(9.59f, 13.91f, 9.28f, 13.92f)
            reflectiveQuadTo(8.77f, 14.19f)
            lineTo(7.44f, 15.96f)
            quadTo(7.26f, 16.18f, 7.39f, 16.42f)
            reflectiveQuadToRelative(0.38f, 0.23f)
            horizontalLineToRelative(8.56f)
            quadToRelative(0.26f, 0f, 0.38f, -0.23f)
            reflectiveQuadTo(16.68f, 15.97f)
            lineTo(14.2f, 12.68f)
            quadTo(14.12f, 12.66f, 14.03f, 12.64f)
            reflectiveQuadTo(13.86f, 12.58f)
            lineTo(11.23f, 16f)
            close()
            moveTo(5.39f, 20.32f)
            quadToRelative(-0.71f, 0f, -1.21f, -0.5f)
            reflectiveQuadTo(3.68f, 18.61f)
            verticalLineTo(5.39f)
            quadToRelative(0f, -0.71f, 0.5f, -1.21f)
            reflectiveQuadTo(5.39f, 3.68f)
            horizontalLineTo(9.34f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            reflectiveQuadTo(9.84f, 4.88f)
            reflectiveQuadTo(9.34f, 5.08f)
            horizontalLineTo(5.39f)
            quadToRelative(-0.12f, 0f, -0.21f, 0.1f)
            reflectiveQuadTo(5.08f, 5.39f)
            verticalLineTo(18.61f)
            quadToRelative(0f, 0.12f, 0.1f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.1f)
            horizontalLineTo(18.61f)
            quadToRelative(0.12f, 0f, 0.21f, -0.1f)
            reflectiveQuadToRelative(0.1f, -0.21f)
            verticalLineTo(14.83f)
            quadToRelative(0f, -0.29f, 0.21f, -0.5f)
            reflectiveQuadToRelative(0.5f, -0.21f)
            reflectiveQuadToRelative(0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.5f)
            verticalLineToRelative(3.78f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(5.39f)
            close()
            moveTo(12f, 12f)
            close()
            moveToRelative(4.05f, -1.58f)
            quadToRelative(-1.61f, 0f, -2.74f, -1.13f)
            reflectiveQuadTo(12.18f, 6.54f)
            reflectiveQuadTo(13.31f, 3.79f)
            reflectiveQuadTo(16.06f, 2.66f)
            reflectiveQuadToRelative(2.75f, 1.13f)
            reflectiveQuadToRelative(1.13f, 2.75f)
            quadToRelative(0f, 0.62f, -0.19f, 1.21f)
            reflectiveQuadTo(19.24f, 8.84f)
            lineToRelative(2.53f, 2.53f)
            quadToRelative(0.2f, 0.2f, 0.2f, 0.48f)
            reflectiveQuadToRelative(-0.21f, 0.5f)
            reflectiveQuadToRelative(-0.49f, 0.21f)
            reflectiveQuadTo(20.79f, 12.34f)
            lineTo(18.2f, 9.76f)
            quadToRelative(-0.5f, 0.36f, -1.01f, 0.51f)
            reflectiveQuadToRelative(-1.13f, 0.15f)
            close()
            moveToRelative(0.01f, -1.4f)
            quadToRelative(1.02f, 0f, 1.75f, -0.72f)
            reflectiveQuadTo(18.54f, 6.54f)
            reflectiveQuadTo(17.82f, 4.78f)
            reflectiveQuadTo(16.06f, 4.06f)
            reflectiveQuadTo(14.3f, 4.78f)
            reflectiveQuadTo(13.58f, 6.54f)
            reflectiveQuadTo(14.3f, 8.3f)
            reflectiveQuadToRelative(1.76f, 0.72f)
            close()
          }
        }
        .build().also { _imageSearchW300 = it }
        IconWeight.W400 -> _imageSearchW400 ?: ImageVector.Builder(
          name = "image_search",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.25f, 16f)
            lineTo(9.6f, 13.8f)
            quadTo(9.38f, 13.5f, 9f, 13.5f)
            reflectiveQuadTo(8.4f, 13.8f)
            lineTo(6.6f, 16.2f)
            quadTo(6.4f, 16.45f, 6.55f, 16.73f)
            reflectiveQuadTo(7f, 17f)
            horizontalLineTo(17f)
            quadToRelative(0.3f, 0f, 0.45f, -0.27f)
            reflectiveQuadTo(17.4f, 16.2f)
            lineTo(14.93f, 12.9f)
            quadTo(14.65f, 12.85f, 14.36f, 12.77f)
            reflectiveQuadTo(13.8f, 12.6f)
            lineTo(11.25f, 16f)
            close()
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineTo(9f)
            quadTo(9.43f, 3f, 9.71f, 3.29f)
            reflectiveQuadTo(10f, 4f)
            quadTo(10f, 4.42f, 9.71f, 4.71f)
            reflectiveQuadTo(9f, 5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(15.68f)
            quadToRelative(0f, -0.42f, 0.29f, -0.71f)
            reflectiveQuadTo(20f, 14.68f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 15.68f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveToRelative(7f, -9f)
            close()
            moveToRelative(4.05f, -1f)
            quadTo(14.2f, 11f, 12.9f, 9.69f)
            reflectiveQuadTo(11.6f, 6.5f)
            reflectiveQuadTo(12.91f, 3.31f)
            reflectiveQuadTo(16.1f, 2f)
            reflectiveQuadToRelative(3.19f, 1.31f)
            reflectiveQuadTo(20.6f, 6.5f)
            quadToRelative(0f, 0.68f, -0.2f, 1.3f)
            quadTo(20.2f, 8.42f, 19.9f, 8.95f)
            lineToRelative(2.35f, 2.35f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
            reflectiveQuadToRelative(-0.28f, 0.7f)
            reflectiveQuadToRelative(-0.7f, 0.28f)
            reflectiveQuadTo(20.85f, 12.7f)
            lineToRelative(-2.4f, -2.4f)
            quadToRelative(-0.52f, 0.35f, -1.13f, 0.53f)
            quadTo(16.73f, 11f, 16.05f, 11f)
            close()
            moveTo(16.1f, 9f)
            quadToRelative(1.05f, 0f, 1.77f, -0.73f)
            reflectiveQuadTo(18.6f, 6.5f)
            reflectiveQuadTo(17.88f, 4.72f)
            reflectiveQuadTo(16.1f, 4f)
            reflectiveQuadTo(14.33f, 4.72f)
            reflectiveQuadTo(13.6f, 6.5f)
            reflectiveQuadToRelative(0.72f, 1.77f)
            reflectiveQuadTo(16.1f, 9f)
            close()
          }
        }
        .build().also { _imageSearchW400 = it }
        IconWeight.W500 -> _imageSearchW500 ?: ImageVector.Builder(
          name = "image_search",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.25f, 15.81f)
            lineTo(9.69f, 13.73f)
            quadTo(9.43f, 13.38f, 9f, 13.38f)
            reflectiveQuadTo(8.32f, 13.73f)
            lineTo(6.57f, 16.09f)
            quadToRelative(-0.22f, 0.29f, -0.06f, 0.6f)
            reflectiveQuadTo(7.02f, 17f)
            horizontalLineToRelative(9.95f)
            quadToRelative(0.35f, 0f, 0.51f, -0.31f)
            reflectiveQuadToRelative(-0.06f, -0.6f)
            lineTo(15.07f, 12.92f)
            quadToRelative(-0.35f, -0.05f, -0.7f, -0.14f)
            reflectiveQuadTo(13.69f, 12.55f)
            lineToRelative(-2.44f, 3.26f)
            close()
            moveTo(5.07f, 21.2f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(2.8f, 18.93f)
            verticalLineTo(5.07f)
            quadTo(2.8f, 4.13f, 3.46f, 3.46f)
            reflectiveQuadTo(5.07f, 2.8f)
            horizontalLineTo(8.86f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(10f, 3.93f)
            reflectiveQuadTo(9.67f, 4.74f)
            reflectiveQuadTo(8.86f, 5.07f)
            horizontalLineTo(5.07f)
            verticalLineTo(18.93f)
            horizontalLineTo(18.93f)
            verticalLineTo(15.88f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadToRelative(0.81f, -0.33f)
            reflectiveQuadToRelative(0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            verticalLineToRelative(3.05f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(18.93f, 21.2f)
            horizontalLineTo(5.07f)
            close()
            moveTo(12f, 12f)
            close()
            moveToRelative(4.05f, -1f)
            quadTo(14.2f, 11f, 12.9f, 9.69f)
            reflectiveQuadTo(11.6f, 6.5f)
            reflectiveQuadTo(12.91f, 3.31f)
            reflectiveQuadTo(16.1f, 2f)
            reflectiveQuadToRelative(3.19f, 1.31f)
            reflectiveQuadTo(20.6f, 6.5f)
            quadToRelative(0f, 0.68f, -0.2f, 1.3f)
            quadTo(20.2f, 8.42f, 19.9f, 8.95f)
            lineToRelative(2.35f, 2.35f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
            reflectiveQuadToRelative(-0.28f, 0.7f)
            reflectiveQuadToRelative(-0.7f, 0.28f)
            reflectiveQuadTo(20.85f, 12.7f)
            lineToRelative(-2.4f, -2.4f)
            quadToRelative(-0.52f, 0.35f, -1.13f, 0.53f)
            quadTo(16.73f, 11f, 16.05f, 11f)
            close()
            moveTo(16.1f, 9f)
            quadToRelative(1.05f, 0f, 1.77f, -0.73f)
            reflectiveQuadTo(18.6f, 6.5f)
            reflectiveQuadTo(17.88f, 4.72f)
            reflectiveQuadTo(16.1f, 4f)
            reflectiveQuadTo(14.33f, 4.72f)
            reflectiveQuadTo(13.6f, 6.5f)
            reflectiveQuadToRelative(0.72f, 1.77f)
            reflectiveQuadTo(16.1f, 9f)
            close()
          }
        }
        .build().also { _imageSearchW500 = it }
    }

private var _imageSearchW300: ImageVector? = null
private var _imageSearchW400: ImageVector? = null
private var _imageSearchW500: ImageVector? = null

public val FujiIcons.Info: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _infoW300 ?: ImageVector.Builder(
          name = "info",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.5f, 16.44f)
            quadToRelative(0.21f, -0.21f, 0.21f, -0.5f)
            verticalLineTo(11.72f)
            quadToRelative(0f, -0.29f, -0.21f, -0.49f)
            reflectiveQuadTo(12.01f, 11.02f)
            reflectiveQuadToRelative(-0.49f, 0.21f)
            reflectiveQuadToRelative(-0.21f, 0.49f)
            verticalLineToRelative(4.23f)
            quadToRelative(0f, 0.29f, 0.21f, 0.5f)
            reflectiveQuadToRelative(0.49f, 0.21f)
            reflectiveQuadTo(12.5f, 16.44f)
            close()
            moveTo(12.55f, 9.15f)
            quadTo(12.77f, 8.94f, 12.77f, 8.6f)
            reflectiveQuadTo(12.55f, 8.05f)
            reflectiveQuadTo(12f, 7.83f)
            reflectiveQuadTo(11.45f, 8.05f)
            reflectiveQuadTo(11.23f, 8.6f)
            reflectiveQuadToRelative(0.22f, 0.55f)
            reflectiveQuadTo(12f, 9.37f)
            reflectiveQuadTo(12.55f, 9.15f)
            close()
            moveTo(12f, 21.3f)
            quadToRelative(-1.93f, 0f, -3.63f, -0.73f)
            reflectiveQuadTo(5.42f, 18.58f)
            reflectiveQuadTo(3.43f, 15.62f)
            reflectiveQuadTo(2.7f, 12f)
            quadToRelative(0f, -1.93f, 0.73f, -3.63f)
            reflectiveQuadTo(5.42f, 5.42f)
            reflectiveQuadTo(8.38f, 3.43f)
            reflectiveQuadTo(12f, 2.7f)
            quadToRelative(1.93f, 0f, 3.63f, 0.73f)
            reflectiveQuadToRelative(2.95f, 1.99f)
            reflectiveQuadToRelative(1.99f, 2.95f)
            reflectiveQuadTo(21.3f, 12f)
            quadToRelative(0f, 1.93f, -0.73f, 3.63f)
            reflectiveQuadToRelative(-1.99f, 2.95f)
            reflectiveQuadToRelative(-2.95f, 1.99f)
            reflectiveQuadTo(12f, 21.3f)
            close()
            moveTo(12f, 19.9f)
            quadToRelative(3.3f, 0f, 5.6f, -2.3f)
            reflectiveQuadTo(19.9f, 12f)
            reflectiveQuadTo(17.6f, 6.4f)
            reflectiveQuadTo(12f, 4.1f)
            reflectiveQuadTo(6.4f, 6.4f)
            reflectiveQuadTo(4.1f, 12f)
            reflectiveQuadToRelative(2.3f, 5.6f)
            reflectiveQuadTo(12f, 19.9f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build().also { _infoW300 = it }
        IconWeight.W400 -> _infoW400 ?: ImageVector.Builder(
          name = "info",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.71f, 16.71f)
            quadTo(13f, 16.43f, 13f, 16f)
            verticalLineTo(12f)
            quadToRelative(0f, -0.43f, -0.29f, -0.71f)
            reflectiveQuadTo(12f, 11f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(11f, 12f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.43f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 17f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(0f, -8f)
            quadTo(13f, 8.42f, 13f, 8f)
            quadTo(13f, 7.57f, 12.71f, 7.29f)
            reflectiveQuadTo(12f, 7f)
            reflectiveQuadTo(11.29f, 7.29f)
            reflectiveQuadTo(11f, 8f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 9f)
            reflectiveQuadTo(12.71f, 8.71f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(3.35f, 0f, 5.68f, -2.32f)
            reflectiveQuadTo(20f, 12f)
            reflectiveQuadTo(17.68f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(6.33f, 6.32f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(0f, -8f)
            close()
          }
        }
        .build().also { _infoW400 = it }
        IconWeight.W500 -> _infoW500 ?: ImageVector.Builder(
          name = "info",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.78f, 16.78f)
            quadTo(13.09f, 16.46f, 13.09f, 16f)
            verticalLineTo(12.09f)
            quadToRelative(0f, -0.46f, -0.31f, -0.78f)
            reflectiveQuadTo(12f, 11f)
            reflectiveQuadToRelative(-0.78f, 0.31f)
            reflectiveQuadToRelative(-0.31f, 0.78f)
            verticalLineTo(16f)
            quadToRelative(0f, 0.46f, 0.31f, 0.78f)
            reflectiveQuadTo(12f, 17.09f)
            reflectiveQuadToRelative(0.78f, -0.31f)
            close()
            moveTo(12.8f, 8.8f)
            quadTo(13.12f, 8.48f, 13.12f, 8f)
            reflectiveQuadTo(12.8f, 7.2f)
            reflectiveQuadTo(12f, 6.88f)
            reflectiveQuadTo(11.2f, 7.2f)
            reflectiveQuadTo(10.88f, 8f)
            reflectiveQuadTo(11.2f, 8.8f)
            reflectiveQuadTo(12f, 9.12f)
            reflectiveQuadTo(12.8f, 8.8f)
            close()
            moveTo(12f, 22.2f)
            quadToRelative(-2.12f, 0f, -3.98f, -0.8f)
            reflectiveQuadTo(4.78f, 19.22f)
            reflectiveQuadTo(2.6f, 15.98f)
            reflectiveQuadTo(1.8f, 12f)
            reflectiveQuadTo(2.6f, 8.02f)
            reflectiveQuadTo(4.78f, 4.78f)
            reflectiveQuadTo(8.02f, 2.6f)
            reflectiveQuadTo(12f, 1.8f)
            reflectiveQuadToRelative(3.98f, 0.8f)
            reflectiveQuadToRelative(3.24f, 2.18f)
            reflectiveQuadTo(21.4f, 8.02f)
            reflectiveQuadTo(22.2f, 12f)
            reflectiveQuadToRelative(-0.8f, 3.98f)
            reflectiveQuadToRelative(-2.18f, 3.24f)
            reflectiveQuadTo(15.98f, 21.4f)
            reflectiveQuadTo(12f, 22.2f)
            close()
            moveToRelative(0f, -2.28f)
            quadToRelative(3.33f, 0f, 5.63f, -2.3f)
            reflectiveQuadTo(19.93f, 12f)
            reflectiveQuadTo(17.63f, 6.37f)
            reflectiveQuadTo(12f, 4.07f)
            reflectiveQuadTo(6.37f, 6.37f)
            reflectiveQuadTo(4.07f, 12f)
            reflectiveQuadToRelative(2.3f, 5.63f)
            reflectiveQuadTo(12f, 19.93f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build().also { _infoW500 = it }
    }

private var _infoW300: ImageVector? = null
private var _infoW400: ImageVector? = null
private var _infoW500: ImageVector? = null

public val FujiIcons.KeyboardArrowDown: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _keyboardArrowDownW300 ?: ImageVector.Builder(
          name = "keyboard_arrow_down",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.68f, 14.54f)
            quadTo(11.53f, 14.48f, 11.4f, 14.35f)
            lineTo(6.97f, 9.91f)
            quadTo(6.76f, 9.71f, 6.76f, 9.42f)
            reflectiveQuadTo(6.97f, 8.93f)
            reflectiveQuadTo(7.46f, 8.73f)
            reflectiveQuadToRelative(0.48f, 0.2f)
            lineTo(12f, 12.99f)
            lineTo(16.05f, 8.93f)
            quadToRelative(0.2f, -0.2f, 0.48f, -0.2f)
            reflectiveQuadToRelative(0.5f, 0.2f)
            reflectiveQuadToRelative(0.22f, 0.49f)
            reflectiveQuadTo(17.04f, 9.91f)
            lineTo(12.6f, 14.35f)
            quadToRelative(-0.13f, 0.13f, -0.28f, 0.19f)
            reflectiveQuadTo(12f, 14.6f)
            reflectiveQuadTo(11.68f, 14.54f)
            close()
          }
        }
        .build().also { _keyboardArrowDownW300 = it }
        IconWeight.W400 -> _keyboardArrowDownW400 ?: ImageVector.Builder(
          name = "keyboard_arrow_down",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.63f, 14.91f)
            quadTo(11.45f, 14.85f, 11.3f, 14.7f)
            lineTo(6.7f, 10.1f)
            quadTo(6.43f, 9.82f, 6.43f, 9.4f)
            quadTo(6.43f, 8.98f, 6.7f, 8.7f)
            reflectiveQuadTo(7.4f, 8.42f)
            reflectiveQuadTo(8.1f, 8.7f)
            lineTo(12f, 12.6f)
            lineTo(15.9f, 8.7f)
            quadTo(16.18f, 8.42f, 16.6f, 8.42f)
            reflectiveQuadTo(17.3f, 8.7f)
            reflectiveQuadToRelative(0.27f, 0.7f)
            reflectiveQuadTo(17.3f, 10.1f)
            lineToRelative(-4.6f, 4.6f)
            quadToRelative(-0.15f, 0.15f, -0.33f, 0.21f)
            reflectiveQuadTo(12f, 14.98f)
            reflectiveQuadTo(11.63f, 14.91f)
            close()
          }
        }
        .build().also { _keyboardArrowDownW400 = it }
        IconWeight.W500 -> _keyboardArrowDownW500 ?: ImageVector.Builder(
          name = "keyboard_arrow_down",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.57f, 15.04f)
            quadTo(11.37f, 14.96f, 11.2f, 14.79f)
            lineTo(6.6f, 10.2f)
            quadTo(6.29f, 9.88f, 6.29f, 9.4f)
            reflectiveQuadTo(6.6f, 8.6f)
            reflectiveQuadTo(7.4f, 8.29f)
            reflectiveQuadTo(8.2f, 8.6f)
            lineToRelative(3.8f, 3.8f)
            lineTo(15.8f, 8.6f)
            quadTo(16.12f, 8.29f, 16.6f, 8.29f)
            reflectiveQuadTo(17.4f, 8.6f)
            reflectiveQuadToRelative(0.32f, 0.8f)
            reflectiveQuadTo(17.4f, 10.2f)
            lineTo(12.8f, 14.79f)
            quadToRelative(-0.17f, 0.17f, -0.37f, 0.25f)
            reflectiveQuadTo(12f, 15.11f)
            reflectiveQuadTo(11.57f, 15.04f)
            close()
          }
        }
        .build().also { _keyboardArrowDownW500 = it }
    }

private var _keyboardArrowDownW300: ImageVector? = null
private var _keyboardArrowDownW400: ImageVector? = null
private var _keyboardArrowDownW500: ImageVector? = null

public val FujiIcons.KeyboardArrowRight: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _keyboardArrowRightW300 ?: ImageVector.Builder(
          name = "keyboard_arrow_right",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.99f, 12f)
            lineTo(8.93f, 7.95f)
            quadTo(8.74f, 7.75f, 8.73f, 7.47f)
            reflectiveQuadToRelative(0.2f, -0.5f)
            reflectiveQuadTo(9.42f, 6.76f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            lineToRelative(4.43f, 4.43f)
            quadToRelative(0.13f, 0.13f, 0.19f, 0.28f)
            reflectiveQuadTo(14.61f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.32f)
            reflectiveQuadTo(14.35f, 12.6f)
            lineTo(9.92f, 17.03f)
            quadTo(9.71f, 17.24f, 9.43f, 17.24f)
            reflectiveQuadTo(8.93f, 17.03f)
            reflectiveQuadTo(8.73f, 16.54f)
            reflectiveQuadToRelative(0.2f, -0.48f)
            lineTo(12.99f, 12f)
            close()
          }
        }
        .build().also { _keyboardArrowRightW300 = it }
        IconWeight.W400 -> _keyboardArrowRightW400 ?: ImageVector.Builder(
          name = "keyboard_arrow_right",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.6f, 12f)
            lineTo(8.7f, 8.1f)
            quadTo(8.43f, 7.82f, 8.43f, 7.4f)
            reflectiveQuadTo(8.7f, 6.7f)
            reflectiveQuadTo(9.4f, 6.43f)
            reflectiveQuadTo(10.1f, 6.7f)
            lineToRelative(4.6f, 4.6f)
            quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
            reflectiveQuadTo(14.98f, 12f)
            reflectiveQuadToRelative(-0.06f, 0.38f)
            reflectiveQuadTo(14.7f, 12.7f)
            lineToRelative(-4.6f, 4.6f)
            quadTo(9.83f, 17.58f, 9.4f, 17.58f)
            reflectiveQuadTo(8.7f, 17.3f)
            quadTo(8.43f, 17.02f, 8.43f, 16.6f)
            reflectiveQuadTo(8.7f, 15.9f)
            lineTo(12.6f, 12f)
            close()
          }
        }
        .build().also { _keyboardArrowRightW400 = it }
        IconWeight.W500 -> _keyboardArrowRightW500 ?: ImageVector.Builder(
          name = "keyboard_arrow_right",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.41f, 12f)
            lineTo(8.6f, 8.2f)
            quadTo(8.29f, 7.88f, 8.29f, 7.4f)
            reflectiveQuadTo(8.6f, 6.6f)
            reflectiveQuadTo(9.4f, 6.29f)
            reflectiveQuadTo(10.2f, 6.6f)
            lineToRelative(4.59f, 4.59f)
            quadToRelative(0.17f, 0.17f, 0.25f, 0.37f)
            reflectiveQuadTo(15.11f, 12f)
            reflectiveQuadToRelative(-0.08f, 0.43f)
            reflectiveQuadTo(14.79f, 12.8f)
            lineTo(10.2f, 17.4f)
            quadTo(9.88f, 17.71f, 9.4f, 17.71f)
            reflectiveQuadTo(8.6f, 17.4f)
            reflectiveQuadTo(8.29f, 16.6f)
            reflectiveQuadTo(8.6f, 15.8f)
            lineTo(12.41f, 12f)
            close()
          }
        }
        .build().also { _keyboardArrowRightW500 = it }
    }

private var _keyboardArrowRightW300: ImageVector? = null
private var _keyboardArrowRightW400: ImageVector? = null
private var _keyboardArrowRightW500: ImageVector? = null

public val FujiIcons.Label: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _labelW300 ?: ImageVector.Builder(
          name = "label",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.41f, 19.3f)
            quadTo(3.7f, 19.3f, 3.2f, 18.8f)
            reflectiveQuadTo(2.7f, 17.59f)
            verticalLineTo(6.42f)
            quadToRelative(0f, -0.7f, 0.5f, -1.21f)
            reflectiveQuadTo(4.41f, 4.71f)
            horizontalLineTo(14.89f)
            quadToRelative(0.41f, 0f, 0.77f, 0.18f)
            reflectiveQuadToRelative(0.6f, 0.49f)
            lineToRelative(4.26f, 5.58f)
            quadToRelative(0.35f, 0.46f, 0.35f, 1.03f)
            reflectiveQuadToRelative(-0.35f, 1.03f)
            lineToRelative(-4.26f, 5.59f)
            quadToRelative(-0.23f, 0.32f, -0.6f, 0.49f)
            reflectiveQuadTo(14.89f, 19.3f)
            horizontalLineTo(4.41f)
            close()
            moveToRelative(0f, -1.4f)
            horizontalLineTo(14.89f)
            quadToRelative(0.07f, 0f, 0.14f, -0.03f)
            reflectiveQuadToRelative(0.11f, -0.09f)
            lineTo(19.4f, 12.19f)
            quadTo(19.47f, 12.11f, 19.47f, 12f)
            reflectiveQuadTo(19.4f, 11.81f)
            lineTo(15.14f, 6.22f)
            quadTo(15.09f, 6.17f, 15.03f, 6.14f)
            reflectiveQuadTo(14.89f, 6.11f)
            horizontalLineTo(4.41f)
            quadTo(4.27f, 6.11f, 4.19f, 6.2f)
            reflectiveQuadTo(4.1f, 6.42f)
            verticalLineTo(17.59f)
            quadToRelative(0f, 0.13f, 0.09f, 0.22f)
            reflectiveQuadTo(4.41f, 17.9f)
            close()
            moveTo(11.79f, 12f)
            close()
          }
        }
        .build().also { _labelW300 = it }
        IconWeight.W400 -> _labelW400 ?: ImageVector.Builder(
          name = "label",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(15f)
            quadToRelative(0.48f, 0f, 0.9f, 0.21f)
            reflectiveQuadTo(16.6f, 4.8f)
            lineToRelative(4.5f, 6f)
            quadToRelative(0.4f, 0.53f, 0.4f, 1.2f)
            reflectiveQuadToRelative(-0.4f, 1.2f)
            lineToRelative(-4.5f, 6f)
            quadToRelative(-0.28f, 0.38f, -0.7f, 0.59f)
            reflectiveQuadTo(15f, 20f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 18f)
            horizontalLineTo(15f)
            lineToRelative(4.5f, -6f)
            lineTo(15f, 6f)
            horizontalLineTo(4f)
            verticalLineTo(18f)
            close()
            moveToRelative(7.75f, -6f)
            close()
          }
        }
        .build().also { _labelW400 = it }
        IconWeight.W500 -> _labelW500 ?: ImageVector.Builder(
          name = "label",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.07f, 20.2f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(1.8f, 17.93f)
            verticalLineTo(6.07f)
            quadTo(1.8f, 5.13f, 2.46f, 4.46f)
            reflectiveQuadTo(4.07f, 3.8f)
            horizontalLineTo(14.93f)
            quadToRelative(0.54f, 0f, 1.03f, 0.24f)
            reflectiveQuadToRelative(0.8f, 0.67f)
            lineToRelative(4.43f, 5.93f)
            quadToRelative(0.45f, 0.6f, 0.45f, 1.36f)
            reflectiveQuadToRelative(-0.45f, 1.36f)
            lineToRelative(-4.43f, 5.93f)
            quadToRelative(-0.31f, 0.43f, -0.8f, 0.67f)
            reflectiveQuadTo(14.93f, 20.2f)
            horizontalLineTo(4.07f)
            close()
            moveTo(4.07f, 17.93f)
            horizontalLineTo(14.94f)
            lineTo(19.37f, 12f)
            lineTo(14.94f, 6.07f)
            horizontalLineTo(4.07f)
            verticalLineTo(17.93f)
            close()
            moveTo(11.72f, 12f)
            close()
          }
        }
        .build().also { _labelW500 = it }
    }

private var _labelW300: ImageVector? = null
private var _labelW400: ImageVector? = null
private var _labelW500: ImageVector? = null

public val FujiIcons.LinkedCamera: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _linkedCameraW300 ?: ImageVector.Builder(
          name = "linked_camera",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(20.2f, 6.79f)
            quadToRelative(0f, -1.67f, -1.16f, -2.84f)
            reflectiveQuadTo(16.2f, 2.78f)
            quadToRelative(-0.21f, 0f, -0.37f, -0.17f)
            reflectiveQuadTo(15.68f, 2.24f)
            reflectiveQuadTo(15.83f, 1.87f)
            reflectiveQuadTo(16.2f, 1.72f)
            quadToRelative(2.11f, 0f, 3.59f, 1.48f)
            reflectiveQuadToRelative(1.48f, 3.59f)
            quadToRelative(0f, 0.21f, -0.15f, 0.37f)
            reflectiveQuadTo(20.74f, 7.31f)
            reflectiveQuadTo(20.36f, 7.16f)
            reflectiveQuadTo(20.2f, 6.79f)
            close()
            moveTo(17.64f, 6.8f)
            quadToRelative(0f, -0.6f, -0.42f, -1.02f)
            reflectiveQuadTo(16.19f, 5.36f)
            quadToRelative(-0.23f, 0f, -0.37f, -0.16f)
            reflectiveQuadTo(15.68f, 4.82f)
            reflectiveQuadTo(15.83f, 4.44f)
            reflectiveQuadTo(16.2f, 4.29f)
            quadToRelative(1.04f, 0f, 1.76f, 0.73f)
            reflectiveQuadTo(18.7f, 6.79f)
            quadTo(18.7f, 7f, 18.54f, 7.16f)
            reflectiveQuadTo(18.17f, 7.31f)
            reflectiveQuadTo(17.8f, 7.16f)
            reflectiveQuadTo(17.64f, 6.8f)
            close()
            moveTo(12f, 12.99f)
            close()
            moveTo(4.41f, 20.3f)
            quadTo(3.7f, 20.3f, 3.2f, 19.8f)
            reflectiveQuadTo(2.7f, 18.59f)
            verticalLineTo(7.39f)
            quadTo(2.7f, 6.69f, 3.2f, 6.19f)
            reflectiveQuadTo(4.41f, 5.69f)
            horizontalLineTo(7.46f)
            lineTo(8.76f, 4.26f)
            quadTo(9f, 4f, 9.33f, 3.85f)
            reflectiveQuadTo(10.03f, 3.7f)
            horizontalLineToRelative(3.96f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadTo(14.69f, 4.4f)
            quadToRelative(0f, 0.29f, -0.21f, 0.5f)
            reflectiveQuadTo(13.99f, 5.1f)
            horizontalLineTo(9.9f)
            lineTo(8.09f, 7.09f)
            horizontalLineTo(4.41f)
            quadToRelative(-0.13f, 0f, -0.22f, 0.09f)
            reflectiveQuadTo(4.1f, 7.4f)
            verticalLineToRelative(11.2f)
            quadToRelative(0f, 0.13f, 0.09f, 0.22f)
            reflectiveQuadTo(4.41f, 18.9f)
            horizontalLineTo(19.59f)
            quadToRelative(0.13f, 0f, 0.22f, -0.09f)
            reflectiveQuadTo(19.9f, 18.59f)
            verticalLineTo(9.01f)
            quadToRelative(0f, -0.29f, 0.21f, -0.49f)
            reflectiveQuadTo(20.6f, 8.31f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadTo(21.3f, 9.01f)
            verticalLineToRelative(9.58f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(4.41f)
            close()
            moveTo(12f, 17.01f)
            quadToRelative(1.69f, 0f, 2.86f, -1.17f)
            reflectiveQuadToRelative(1.17f, -2.86f)
            quadToRelative(0f, -1.68f, -1.17f, -2.85f)
            reflectiveQuadTo(12f, 8.97f)
            reflectiveQuadTo(9.14f, 10.13f)
            reflectiveQuadTo(7.98f, 12.99f)
            quadToRelative(0f, 1.69f, 1.17f, 2.86f)
            reflectiveQuadTo(12f, 17.01f)
            close()
            moveToRelative(-0f, -1.4f)
            quadToRelative(-1.11f, 0f, -1.87f, -0.76f)
            reflectiveQuadTo(9.38f, 12.99f)
            reflectiveQuadToRelative(0.75f, -1.87f)
            reflectiveQuadTo(12f, 10.36f)
            reflectiveQuadToRelative(1.87f, 0.76f)
            reflectiveQuadToRelative(0.76f, 1.87f)
            reflectiveQuadToRelative(-0.76f, 1.87f)
            reflectiveQuadTo(12f, 15.62f)
            close()
          }
        }
        .build().also { _linkedCameraW300 = it }
        IconWeight.W400 -> _linkedCameraW400 ?: ImageVector.Builder(
          name = "linked_camera",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(20.6f, 6.32f)
            quadToRelative(0f, -1.65f, -1.14f, -2.79f)
            reflectiveQuadTo(16.68f, 2.4f)
            quadTo(16.4f, 2.4f, 16.2f, 2.19f)
            quadTo(16f, 1.97f, 16f, 1.7f)
            quadTo(16f, 1.42f, 16.2f, 1.22f)
            reflectiveQuadToRelative(0.48f, -0.2f)
            quadToRelative(2.2f, 0f, 3.75f, 1.55f)
            reflectiveQuadToRelative(1.55f, 3.75f)
            quadToRelative(0f, 0.27f, -0.2f, 0.48f)
            reflectiveQuadTo(21.3f, 7f)
            reflectiveQuadTo(20.81f, 6.8f)
            reflectiveQuadTo(20.6f, 6.32f)
            close()
            moveTo(17.9f, 6.35f)
            quadToRelative(0f, -0.52f, -0.36f, -0.89f)
            reflectiveQuadTo(16.65f, 5.1f)
            quadToRelative(-0.28f, 0f, -0.46f, -0.21f)
            quadTo(16f, 4.67f, 16f, 4.4f)
            reflectiveQuadTo(16.2f, 3.92f)
            reflectiveQuadToRelative(0.48f, -0.2f)
            quadToRelative(1.07f, 0f, 1.84f, 0.76f)
            quadToRelative(0.76f, 0.76f, 0.76f, 1.84f)
            quadToRelative(0f, 0.27f, -0.2f, 0.48f)
            quadTo(18.88f, 7f, 18.6f, 7f)
            reflectiveQuadTo(18.11f, 6.81f)
            reflectiveQuadTo(17.9f, 6.35f)
            close()
            moveTo(12f, 13f)
            close()
            moveTo(4f, 21f)
            quadTo(3.18f, 21f, 2.59f, 20.41f)
            reflectiveQuadTo(2f, 19f)
            verticalLineTo(7f)
            quadTo(2f, 6.18f, 2.59f, 5.59f)
            reflectiveQuadTo(4f, 5f)
            horizontalLineTo(7.15f)
            lineTo(8.4f, 3.65f)
            quadTo(8.68f, 3.35f, 9.06f, 3.17f)
            reflectiveQuadTo(9.88f, 3f)
            horizontalLineTo(14f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 4f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(14f, 5f)
            horizontalLineTo(9.88f)
            lineTo(8.05f, 7f)
            horizontalLineTo(4f)
            verticalLineTo(19f)
            horizontalLineTo(20f)
            verticalLineTo(9f)
            quadTo(20f, 8.57f, 20.29f, 8.29f)
            reflectiveQuadTo(21f, 8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(22f, 9f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 21f)
            horizontalLineTo(4f)
            close()
            moveToRelative(8f, -3.5f)
            quadToRelative(1.88f, 0f, 3.19f, -1.31f)
            reflectiveQuadTo(16.5f, 13f)
            reflectiveQuadTo(15.19f, 9.81f)
            reflectiveQuadTo(12f, 8.5f)
            reflectiveQuadTo(8.81f, 9.81f)
            reflectiveQuadTo(7.5f, 13f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(12f, 17.5f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(-1.05f, 0f, -1.77f, -0.72f)
            reflectiveQuadTo(9.5f, 13f)
            reflectiveQuadToRelative(0.73f, -1.78f)
            reflectiveQuadTo(12f, 10.5f)
            reflectiveQuadToRelative(1.78f, 0.72f)
            reflectiveQuadTo(14.5f, 13f)
            reflectiveQuadToRelative(-0.72f, 1.78f)
            reflectiveQuadTo(12f, 15.5f)
            close()
          }
        }
        .build().also { _linkedCameraW400 = it }
        IconWeight.W500 -> _linkedCameraW500 ?: ImageVector.Builder(
          name = "linked_camera",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(20.67f, 6.32f)
            quadToRelative(0f, -1.68f, -1.16f, -2.84f)
            reflectiveQuadTo(16.68f, 2.33f)
            quadToRelative(-0.3f, 0f, -0.51f, -0.23f)
            reflectiveQuadTo(15.95f, 1.57f)
            reflectiveQuadTo(16.16f, 1.05f)
            reflectiveQuadTo(16.68f, 0.83f)
            quadToRelative(2.28f, 0f, 3.88f, 1.6f)
            reflectiveQuadToRelative(1.6f, 3.88f)
            quadToRelative(0f, 0.3f, -0.22f, 0.52f)
            reflectiveQuadTo(21.43f, 7.05f)
            reflectiveQuadTo(20.9f, 6.84f)
            reflectiveQuadTo(20.67f, 6.32f)
            close()
            moveTo(17.82f, 6.35f)
            quadToRelative(0f, -0.49f, -0.34f, -0.83f)
            reflectiveQuadTo(16.65f, 5.18f)
            quadToRelative(-0.3f, 0f, -0.5f, -0.23f)
            reflectiveQuadTo(15.95f, 4.41f)
            reflectiveQuadTo(16.16f, 3.89f)
            reflectiveQuadTo(16.68f, 3.67f)
            quadToRelative(1.1f, 0f, 1.88f, 0.78f)
            reflectiveQuadToRelative(0.78f, 1.88f)
            quadToRelative(0f, 0.3f, -0.22f, 0.51f)
            reflectiveQuadTo(18.59f, 7.05f)
            reflectiveQuadTo(18.05f, 6.85f)
            reflectiveQuadTo(17.82f, 6.35f)
            close()
            moveTo(12f, 13f)
            close()
            moveTo(4.07f, 21.2f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(1.8f, 18.93f)
            verticalLineTo(7.07f)
            quadTo(1.8f, 6.13f, 2.46f, 5.46f)
            reflectiveQuadTo(4.07f, 4.8f)
            horizontalLineTo(7.04f)
            lineTo(8.3f, 3.49f)
            quadTo(8.62f, 3.16f, 9.04f, 2.98f)
            reflectiveQuadTo(9.93f, 2.8f)
            horizontalLineToRelative(3.87f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadTo(13.81f, 5.07f)
            horizontalLineTo(9.94f)
            lineToRelative(-1.93f, 2f)
            horizontalLineTo(4.07f)
            verticalLineTo(18.93f)
            horizontalLineTo(19.93f)
            verticalLineTo(9.19f)
            quadToRelative(0f, -0.48f, 0.33f, -0.81f)
            reflectiveQuadTo(21.07f, 8.05f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(22.2f, 9.19f)
            verticalLineToRelative(9.74f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(19.93f, 21.2f)
            horizontalLineTo(4.07f)
            close()
            moveTo(12f, 17.55f)
            quadToRelative(1.89f, 0f, 3.22f, -1.33f)
            reflectiveQuadTo(16.55f, 13f)
            reflectiveQuadTo(15.22f, 9.78f)
            reflectiveQuadTo(12f, 8.45f)
            reflectiveQuadTo(8.78f, 9.78f)
            reflectiveQuadTo(7.45f, 13f)
            reflectiveQuadToRelative(1.33f, 3.22f)
            reflectiveQuadTo(12f, 17.55f)
            close()
            moveToRelative(0f, -2.16f)
            quadToRelative(-1.01f, 0f, -1.7f, -0.69f)
            reflectiveQuadTo(9.61f, 13f)
            reflectiveQuadTo(10.3f, 11.3f)
            reflectiveQuadTo(12f, 10.61f)
            reflectiveQuadToRelative(1.7f, 0.69f)
            reflectiveQuadTo(14.39f, 13f)
            reflectiveQuadTo(13.7f, 14.7f)
            reflectiveQuadTo(12f, 15.39f)
            close()
          }
        }
        .build().also { _linkedCameraW500 = it }
    }

private var _linkedCameraW300: ImageVector? = null
private var _linkedCameraW400: ImageVector? = null
private var _linkedCameraW500: ImageVector? = null

public val FujiIcons.MoreHoriz: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _moreHorizW300 ?: ImageVector.Builder(
          name = "more_horiz",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.99f, 13.46f)
            quadToRelative(-0.61f, 0f, -1.03f, -0.43f)
            reflectiveQuadTo(4.53f, 12f)
            reflectiveQuadTo(4.96f, 10.97f)
            reflectiveQuadTo(5.99f, 10.54f)
            reflectiveQuadToRelative(1.03f, 0.43f)
            reflectiveQuadTo(7.45f, 12f)
            reflectiveQuadTo(7.02f, 13.03f)
            reflectiveQuadTo(5.99f, 13.46f)
            close()
            moveToRelative(6.01f, 0f)
            quadToRelative(-0.61f, 0f, -1.03f, -0.43f)
            reflectiveQuadTo(10.54f, 12f)
            reflectiveQuadToRelative(0.43f, -1.03f)
            reflectiveQuadTo(12f, 10.54f)
            reflectiveQuadToRelative(1.03f, 0.43f)
            reflectiveQuadTo(13.46f, 12f)
            reflectiveQuadToRelative(-0.43f, 1.03f)
            reflectiveQuadTo(12f, 13.46f)
            close()
            moveToRelative(6.01f, 0f)
            quadToRelative(-0.61f, 0f, -1.03f, -0.43f)
            reflectiveQuadTo(16.55f, 12f)
            reflectiveQuadToRelative(0.43f, -1.03f)
            reflectiveQuadToRelative(1.03f, -0.43f)
            reflectiveQuadToRelative(1.03f, 0.43f)
            reflectiveQuadTo(19.47f, 12f)
            reflectiveQuadToRelative(-0.43f, 1.03f)
            reflectiveQuadToRelative(-1.03f, 0.43f)
            close()
          }
        }
        .build().also { _moreHorizW300 = it }
        IconWeight.W400 -> _moreHorizW400 ?: ImageVector.Builder(
          name = "more_horiz",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6f, 14f)
            quadTo(5.18f, 14f, 4.59f, 13.41f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadTo(4.59f, 10.59f)
            reflectiveQuadTo(6f, 10f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(8f, 11.18f, 8f, 12f)
            reflectiveQuadTo(7.41f, 13.41f)
            reflectiveQuadTo(6f, 14f)
            close()
            moveToRelative(6f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(10f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(14f, 11.18f, 14f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(12f, 14f)
            close()
            moveToRelative(6f, 0f)
            quadToRelative(-0.82f, 0f, -1.41f, -0.59f)
            reflectiveQuadTo(16f, 12f)
            reflectiveQuadToRelative(0.59f, -1.41f)
            reflectiveQuadTo(18f, 10f)
            reflectiveQuadToRelative(1.41f, 0.59f)
            quadTo(20f, 11.18f, 20f, 12f)
            reflectiveQuadToRelative(-0.59f, 1.41f)
            reflectiveQuadTo(18f, 14f)
            close()
          }
        }
        .build().also { _moreHorizW400 = it }
        IconWeight.W500 -> _moreHorizW500 ?: ImageVector.Builder(
          name = "more_horiz",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.82f, 14.09f)
            quadToRelative(-0.86f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(3.72f, 12f)
            quadToRelative(0f, -0.87f, 0.61f, -1.48f)
            reflectiveQuadTo(5.81f, 9.91f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadTo(7.91f, 12f)
            reflectiveQuadTo(7.3f, 13.48f)
            reflectiveQuadTo(5.82f, 14.09f)
            close()
            moveToRelative(6.19f, 0f)
            quadToRelative(-0.86f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(9.91f, 12f)
            quadToRelative(0f, -0.87f, 0.61f, -1.48f)
            reflectiveQuadTo(12f, 9.91f)
            quadToRelative(0.87f, 0f, 1.48f, 0.61f)
            reflectiveQuadTo(14.09f, 12f)
            reflectiveQuadToRelative(-0.61f, 1.48f)
            reflectiveQuadTo(12f, 14.09f)
            close()
            moveToRelative(6.18f, 0f)
            quadToRelative(-0.87f, 0f, -1.48f, -0.61f)
            reflectiveQuadTo(16.09f, 12f)
            quadToRelative(0f, -0.87f, 0.62f, -1.48f)
            reflectiveQuadTo(18.19f, 9.91f)
            reflectiveQuadToRelative(1.48f, 0.61f)
            reflectiveQuadTo(20.28f, 12f)
            reflectiveQuadToRelative(-0.61f, 1.48f)
            reflectiveQuadToRelative(-1.48f, 0.61f)
            close()
          }
        }
        .build().also { _moreHorizW500 = it }
    }

private var _moreHorizW300: ImageVector? = null
private var _moreHorizW400: ImageVector? = null
private var _moreHorizW500: ImageVector? = null

public val FujiIcons.Palette: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _paletteW300 ?: ImageVector.Builder(
          name = "palette",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.98f, 21.3f)
            quadToRelative(-1.91f, 0f, -3.6f, -0.73f)
            reflectiveQuadToRelative(-2.95f, -2f)
            reflectiveQuadTo(3.43f, 15.61f)
            reflectiveQuadTo(2.7f, 12f)
            quadToRelative(0f, -1.95f, 0.75f, -3.64f)
            reflectiveQuadTo(5.49f, 5.41f)
            reflectiveQuadTo(8.52f, 3.43f)
            reflectiveQuadTo(12.22f, 2.7f)
            quadToRelative(1.83f, 0f, 3.47f, 0.63f)
            reflectiveQuadToRelative(2.89f, 1.74f)
            reflectiveQuadToRelative(1.98f, 2.62f)
            reflectiveQuadToRelative(0.74f, 3.28f)
            quadToRelative(0f, 2.54f, -1.5f, 3.96f)
            reflectiveQuadToRelative(-3.86f, 1.42f)
            horizontalLineTo(14.17f)
            quadToRelative(-0.46f, 0f, -0.74f, 0.29f)
            reflectiveQuadToRelative(-0.28f, 0.71f)
            quadToRelative(0f, 0.48f, 0.38f, 1.01f)
            reflectiveQuadToRelative(0.38f, 1.2f)
            quadToRelative(0f, 0.84f, -0.49f, 1.28f)
            reflectiveQuadTo(11.98f, 21.3f)
            close()
            moveTo(12f, 12f)
            close()
            moveTo(7.41f, 12.35f)
            quadTo(7.75f, 12f, 7.75f, 11.5f)
            reflectiveQuadTo(7.41f, 10.65f)
            reflectiveQuadTo(6.56f, 10.31f)
            reflectiveQuadTo(5.71f, 10.65f)
            reflectiveQuadTo(5.37f, 11.5f)
            reflectiveQuadToRelative(0.34f, 0.85f)
            reflectiveQuadToRelative(0.85f, 0.34f)
            reflectiveQuadTo(7.41f, 12.35f)
            close()
            moveTo(10.39f, 8.39f)
            quadTo(10.73f, 8.04f, 10.73f, 7.54f)
            reflectiveQuadTo(10.39f, 6.69f)
            reflectiveQuadTo(9.54f, 6.35f)
            reflectiveQuadTo(8.69f, 6.69f)
            reflectiveQuadTo(8.35f, 7.54f)
            reflectiveQuadTo(8.69f, 8.39f)
            reflectiveQuadTo(9.54f, 8.73f)
            reflectiveQuadTo(10.39f, 8.39f)
            close()
            moveToRelative(4.94f, 0f)
            quadTo(15.67f, 8.04f, 15.67f, 7.54f)
            reflectiveQuadTo(15.33f, 6.69f)
            reflectiveQuadTo(14.48f, 6.35f)
            reflectiveQuadTo(13.63f, 6.69f)
            reflectiveQuadTo(13.29f, 7.54f)
            reflectiveQuadToRelative(0.34f, 0.85f)
            reflectiveQuadToRelative(0.85f, 0.34f)
            reflectiveQuadTo(15.33f, 8.39f)
            close()
            moveToRelative(2.96f, 3.96f)
            quadTo(18.63f, 12f, 18.63f, 11.5f)
            reflectiveQuadTo(18.29f, 10.65f)
            reflectiveQuadTo(17.44f, 10.31f)
            reflectiveQuadToRelative(-0.85f, 0.34f)
            reflectiveQuadTo(16.25f, 11.5f)
            reflectiveQuadToRelative(0.34f, 0.85f)
            reflectiveQuadToRelative(0.85f, 0.34f)
            reflectiveQuadToRelative(0.85f, -0.34f)
            close()
            moveTo(11.98f, 19.9f)
            quadToRelative(0.25f, 0f, 0.38f, -0.12f)
            reflectiveQuadTo(12.5f, 19.45f)
            quadToRelative(0f, -0.35f, -0.36f, -0.8f)
            reflectiveQuadTo(11.77f, 17.3f)
            quadToRelative(0f, -1.06f, 0.71f, -1.7f)
            reflectiveQuadToRelative(1.75f, -0.64f)
            horizontalLineToRelative(1.71f)
            quadToRelative(1.77f, 0f, 2.86f, -1.02f)
            reflectiveQuadToRelative(1.1f, -2.97f)
            quadToRelative(0f, -2.99f, -2.3f, -4.93f)
            reflectiveQuadTo(12.22f, 4.1f)
            quadTo(8.84f, 4.1f, 6.47f, 6.4f)
            reflectiveQuadTo(4.1f, 12f)
            quadToRelative(0f, 3.28f, 2.31f, 5.59f)
            reflectiveQuadToRelative(5.57f, 2.31f)
            close()
          }
        }
        .build().also { _paletteW300 = it }
        IconWeight.W400 -> _paletteW400 ?: ImageVector.Builder(
          name = "palette",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 22f)
            quadTo(9.95f, 22f, 8.13f, 21.21f)
            quadTo(6.3f, 20.43f, 4.94f, 19.06f)
            quadTo(3.58f, 17.7f, 2.79f, 15.88f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.81f, 8.1f)
            quadTo(3.63f, 6.27f, 5.01f, 4.93f)
            quadTo(6.4f, 3.57f, 8.25f, 2.79f)
            reflectiveQuadTo(12.2f, 2f)
            quadToRelative(2f, 0f, 3.78f, 0.69f)
            reflectiveQuadToRelative(3.11f, 1.9f)
            reflectiveQuadToRelative(2.13f, 2.88f)
            quadTo(22f, 9.13f, 22f, 11.05f)
            quadToRelative(0f, 2.88f, -1.75f, 4.41f)
            reflectiveQuadTo(16f, 17f)
            horizontalLineTo(14.15f)
            quadToRelative(-0.22f, 0f, -0.31f, 0.13f)
            reflectiveQuadTo(13.75f, 17.4f)
            quadToRelative(0f, 0.3f, 0.38f, 0.86f)
            reflectiveQuadToRelative(0.38f, 1.29f)
            quadToRelative(0f, 1.25f, -0.69f, 1.85f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(12f, 12f)
            close()
            moveTo(7.58f, 12.58f)
            quadTo(8f, 12.15f, 8f, 11.5f)
            reflectiveQuadTo(7.58f, 10.43f)
            reflectiveQuadTo(6.5f, 10f)
            reflectiveQuadTo(5.43f, 10.43f)
            reflectiveQuadTo(5f, 11.5f)
            reflectiveQuadToRelative(0.43f, 1.07f)
            reflectiveQuadTo(6.5f, 13f)
            reflectiveQuadTo(7.58f, 12.58f)
            close()
            moveToRelative(3f, -4f)
            quadTo(11f, 8.15f, 11f, 7.5f)
            reflectiveQuadTo(10.58f, 6.43f)
            reflectiveQuadTo(9.5f, 6f)
            reflectiveQuadTo(8.43f, 6.43f)
            reflectiveQuadTo(8f, 7.5f)
            reflectiveQuadTo(8.43f, 8.57f)
            reflectiveQuadTo(9.5f, 9f)
            reflectiveQuadTo(10.58f, 8.57f)
            close()
            moveToRelative(5f, 0f)
            quadTo(16f, 8.15f, 16f, 7.5f)
            reflectiveQuadTo(15.58f, 6.43f)
            reflectiveQuadTo(14.5f, 6f)
            reflectiveQuadTo(13.43f, 6.43f)
            reflectiveQuadTo(13f, 7.5f)
            reflectiveQuadToRelative(0.43f, 1.07f)
            reflectiveQuadTo(14.5f, 9f)
            reflectiveQuadTo(15.58f, 8.57f)
            close()
            moveToRelative(3f, 4f)
            quadTo(19f, 12.15f, 19f, 11.5f)
            reflectiveQuadTo(18.58f, 10.43f)
            reflectiveQuadTo(17.5f, 10f)
            reflectiveQuadToRelative(-1.07f, 0.42f)
            reflectiveQuadTo(16f, 11.5f)
            reflectiveQuadToRelative(0.43f, 1.07f)
            reflectiveQuadTo(17.5f, 13f)
            reflectiveQuadToRelative(1.07f, -0.43f)
            close()
            moveTo(12f, 20f)
            quadToRelative(0.23f, 0f, 0.36f, -0.13f)
            reflectiveQuadTo(12.5f, 19.55f)
            quadToRelative(0f, -0.35f, -0.38f, -0.82f)
            reflectiveQuadTo(11.75f, 17.3f)
            quadToRelative(0f, -1.05f, 0.73f, -1.68f)
            reflectiveQuadTo(14.25f, 15f)
            horizontalLineTo(16f)
            quadToRelative(1.65f, 0f, 2.82f, -0.96f)
            reflectiveQuadTo(20f, 11.05f)
            quadTo(20f, 8.02f, 17.69f, 6.01f)
            reflectiveQuadTo(12.2f, 4f)
            quadTo(8.8f, 4f, 6.4f, 6.32f)
            reflectiveQuadTo(4f, 12f)
            quadToRelative(0f, 3.32f, 2.34f, 5.66f)
            reflectiveQuadTo(12f, 20f)
            close()
          }
        }
        .build().also { _paletteW400 = it }
        IconWeight.W500 -> _paletteW500 ?: ImageVector.Builder(
          name = "palette",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 22.2f)
            quadTo(9.91f, 22.2f, 8.05f, 21.4f)
            reflectiveQuadTo(4.8f, 19.2f)
            reflectiveQuadTo(2.6f, 15.95f)
            reflectiveQuadTo(1.8f, 12f)
            quadTo(1.8f, 9.88f, 2.62f, 8.02f)
            reflectiveQuadTo(4.86f, 4.78f)
            reflectiveQuadTo(8.16f, 2.6f)
            reflectiveQuadTo(12.19f, 1.8f)
            quadToRelative(2.04f, 0f, 3.85f, 0.7f)
            reflectiveQuadToRelative(3.18f, 1.94f)
            reflectiveQuadTo(21.4f, 7.38f)
            reflectiveQuadToRelative(0.81f, 3.66f)
            quadToRelative(0f, 2.91f, -1.76f, 4.54f)
            reflectiveQuadTo(16.11f, 17.2f)
            horizontalLineTo(14.4f)
            quadToRelative(-0.2f, 0f, -0.29f, 0.11f)
            reflectiveQuadToRelative(-0.08f, 0.26f)
            quadToRelative(0f, 0.29f, 0.34f, 0.78f)
            reflectiveQuadToRelative(0.34f, 1.19f)
            quadToRelative(0f, 1.3f, -0.75f, 1.98f)
            reflectiveQuadTo(12f, 22.2f)
            close()
            moveTo(12f, 12f)
            close()
            moveTo(7.65f, 12.58f)
            quadTo(8.07f, 12.15f, 8.07f, 11.5f)
            reflectiveQuadTo(7.65f, 10.43f)
            reflectiveQuadTo(6.57f, 10f)
            reflectiveQuadTo(5.5f, 10.43f)
            reflectiveQuadTo(5.07f, 11.5f)
            reflectiveQuadTo(5.5f, 12.58f)
            reflectiveQuadTo(6.57f, 13f)
            reflectiveQuadTo(7.65f, 12.58f)
            close()
            moveTo(10.61f, 8.63f)
            quadTo(11.04f, 8.2f, 11.04f, 7.55f)
            reflectiveQuadTo(10.61f, 6.48f)
            reflectiveQuadTo(9.54f, 6.05f)
            reflectiveQuadTo(8.46f, 6.48f)
            reflectiveQuadTo(8.04f, 7.55f)
            reflectiveQuadTo(8.46f, 8.63f)
            reflectiveQuadTo(9.54f, 9.05f)
            reflectiveQuadTo(10.61f, 8.63f)
            close()
            moveToRelative(4.93f, 0f)
            quadTo(15.96f, 8.2f, 15.96f, 7.55f)
            reflectiveQuadTo(15.54f, 6.48f)
            reflectiveQuadTo(14.46f, 6.05f)
            reflectiveQuadTo(13.39f, 6.48f)
            reflectiveQuadTo(12.96f, 7.55f)
            reflectiveQuadToRelative(0.42f, 1.07f)
            reflectiveQuadToRelative(1.08f, 0.43f)
            reflectiveQuadTo(15.54f, 8.63f)
            close()
            moveToRelative(2.95f, 3.95f)
            quadToRelative(0.43f, -0.43f, 0.43f, -1.07f)
            reflectiveQuadTo(18.49f, 10.43f)
            reflectiveQuadTo(17.41f, 10f)
            reflectiveQuadToRelative(-1.07f, 0.42f)
            reflectiveQuadTo(15.91f, 11.5f)
            reflectiveQuadToRelative(0.43f, 1.07f)
            reflectiveQuadTo(17.41f, 13f)
            reflectiveQuadToRelative(1.07f, -0.43f)
            close()
            moveToRelative(-6.52f, 7.35f)
            quadToRelative(0.21f, 0f, 0.34f, -0.12f)
            reflectiveQuadTo(12.43f, 19.5f)
            quadToRelative(0f, -0.35f, -0.38f, -0.8f)
            reflectiveQuadTo(11.68f, 17.32f)
            quadToRelative(0f, -1.08f, 0.75f, -1.73f)
            reflectiveQuadToRelative(1.83f, -0.65f)
            horizontalLineToRelative(1.86f)
            quadToRelative(1.64f, 0f, 2.73f, -0.96f)
            reflectiveQuadToRelative(1.09f, -2.89f)
            quadToRelative(0f, -3.03f, -2.3f, -5.02f)
            reflectiveQuadTo(12.19f, 4.07f)
            quadToRelative(-3.36f, 0f, -5.74f, 2.31f)
            reflectiveQuadTo(4.07f, 12f)
            quadToRelative(0f, 3.29f, 2.31f, 5.61f)
            reflectiveQuadToRelative(5.58f, 2.32f)
            close()
          }
        }
        .build().also { _paletteW500 = it }
    }

private var _paletteW300: ImageVector? = null
private var _paletteW400: ImageVector? = null
private var _paletteW500: ImageVector? = null

public val FujiIcons.PhotoCamera: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _photoCameraW300 ?: ImageVector.Builder(
          name = "photo_camera",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 17.01f)
            quadToRelative(1.69f, 0f, 2.86f, -1.17f)
            reflectiveQuadToRelative(1.17f, -2.86f)
            quadToRelative(0f, -1.68f, -1.17f, -2.85f)
            reflectiveQuadTo(12f, 8.97f)
            reflectiveQuadTo(9.14f, 10.13f)
            reflectiveQuadTo(7.98f, 12.99f)
            quadToRelative(0f, 1.69f, 1.17f, 2.86f)
            reflectiveQuadTo(12f, 17.01f)
            close()
            moveToRelative(-0f, -1.4f)
            quadToRelative(-1.11f, 0f, -1.87f, -0.76f)
            reflectiveQuadTo(9.38f, 12.99f)
            reflectiveQuadToRelative(0.75f, -1.87f)
            reflectiveQuadTo(12f, 10.36f)
            reflectiveQuadToRelative(1.87f, 0.76f)
            reflectiveQuadToRelative(0.76f, 1.87f)
            reflectiveQuadToRelative(-0.76f, 1.87f)
            reflectiveQuadTo(12f, 15.62f)
            close()
            moveTo(4.41f, 20.3f)
            quadTo(3.7f, 20.3f, 3.2f, 19.8f)
            reflectiveQuadTo(2.7f, 18.59f)
            verticalLineTo(7.39f)
            quadTo(2.7f, 6.69f, 3.2f, 6.19f)
            reflectiveQuadTo(4.41f, 5.69f)
            horizontalLineTo(7.46f)
            lineTo(8.76f, 4.26f)
            quadTo(8.99f, 4f, 9.33f, 3.85f)
            reflectiveQuadTo(10.03f, 3.7f)
            horizontalLineToRelative(3.94f)
            quadToRelative(0.37f, 0f, 0.7f, 0.15f)
            reflectiveQuadToRelative(0.57f, 0.41f)
            lineToRelative(1.3f, 1.43f)
            horizontalLineToRelative(3.05f)
            quadToRelative(0.71f, 0f, 1.21f, 0.5f)
            reflectiveQuadToRelative(0.5f, 1.21f)
            verticalLineToRelative(11.2f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(4.41f)
            close()
            moveToRelative(0f, -1.4f)
            horizontalLineTo(19.59f)
            quadToRelative(0.13f, 0f, 0.22f, -0.09f)
            reflectiveQuadTo(19.9f, 18.59f)
            verticalLineTo(7.4f)
            quadToRelative(0f, -0.13f, -0.09f, -0.22f)
            reflectiveQuadTo(19.59f, 7.09f)
            horizontalLineTo(15.92f)
            lineTo(14.1f, 5.1f)
            horizontalLineTo(9.9f)
            lineTo(8.09f, 7.09f)
            horizontalLineTo(4.41f)
            quadToRelative(-0.13f, 0f, -0.22f, 0.09f)
            reflectiveQuadTo(4.1f, 7.4f)
            verticalLineToRelative(11.2f)
            quadToRelative(0f, 0.13f, 0.09f, 0.22f)
            reflectiveQuadTo(4.41f, 18.9f)
            close()
            moveTo(12f, 12.99f)
            close()
          }
        }
        .build().also { _photoCameraW300 = it }
        IconWeight.W400 -> _photoCameraW400 ?: ImageVector.Builder(
          name = "photo_camera",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 17.5f)
            quadToRelative(1.88f, 0f, 3.19f, -1.31f)
            reflectiveQuadTo(16.5f, 13f)
            reflectiveQuadTo(15.19f, 9.81f)
            reflectiveQuadTo(12f, 8.5f)
            reflectiveQuadTo(8.81f, 9.81f)
            reflectiveQuadTo(7.5f, 13f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(12f, 17.5f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(-1.05f, 0f, -1.77f, -0.72f)
            reflectiveQuadTo(9.5f, 13f)
            reflectiveQuadToRelative(0.73f, -1.78f)
            reflectiveQuadTo(12f, 10.5f)
            reflectiveQuadToRelative(1.78f, 0.72f)
            reflectiveQuadTo(14.5f, 13f)
            reflectiveQuadToRelative(-0.72f, 1.78f)
            reflectiveQuadTo(12f, 15.5f)
            close()
            moveTo(4f, 21f)
            quadTo(3.18f, 21f, 2.59f, 20.41f)
            reflectiveQuadTo(2f, 19f)
            verticalLineTo(7f)
            quadTo(2f, 6.18f, 2.59f, 5.59f)
            reflectiveQuadTo(4f, 5f)
            horizontalLineTo(7.15f)
            lineTo(8.4f, 3.65f)
            quadTo(8.68f, 3.35f, 9.06f, 3.17f)
            reflectiveQuadTo(9.88f, 3f)
            horizontalLineToRelative(4.25f)
            quadToRelative(0.43f, 0f, 0.81f, 0.17f)
            reflectiveQuadTo(15.6f, 3.65f)
            lineTo(16.85f, 5f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 6.18f, 22f, 7f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 21f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 19f)
            horizontalLineTo(20f)
            verticalLineTo(7f)
            horizontalLineTo(15.95f)
            lineTo(14.13f, 5f)
            horizontalLineTo(9.88f)
            lineTo(8.05f, 7f)
            horizontalLineTo(4f)
            verticalLineTo(19f)
            close()
            moveToRelative(8f, -6f)
            close()
          }
        }
        .build().also { _photoCameraW400 = it }
        IconWeight.W500 -> _photoCameraW500 ?: ImageVector.Builder(
          name = "photo_camera",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 17.55f)
            quadToRelative(1.89f, 0f, 3.22f, -1.33f)
            reflectiveQuadTo(16.55f, 13f)
            reflectiveQuadTo(15.22f, 9.78f)
            reflectiveQuadTo(12f, 8.45f)
            reflectiveQuadTo(8.78f, 9.78f)
            reflectiveQuadTo(7.45f, 13f)
            reflectiveQuadToRelative(1.33f, 3.22f)
            reflectiveQuadTo(12f, 17.55f)
            close()
            moveToRelative(0f, -2.16f)
            quadToRelative(-1.01f, 0f, -1.7f, -0.69f)
            reflectiveQuadTo(9.61f, 13f)
            reflectiveQuadTo(10.3f, 11.3f)
            reflectiveQuadTo(12f, 10.61f)
            reflectiveQuadToRelative(1.7f, 0.69f)
            reflectiveQuadTo(14.39f, 13f)
            reflectiveQuadTo(13.7f, 14.7f)
            reflectiveQuadTo(12f, 15.39f)
            close()
            moveTo(4.07f, 21.2f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(1.8f, 18.93f)
            verticalLineTo(7.07f)
            quadTo(1.8f, 6.13f, 2.46f, 5.46f)
            reflectiveQuadTo(4.07f, 4.8f)
            horizontalLineTo(7.04f)
            lineTo(8.3f, 3.49f)
            quadTo(8.62f, 3.16f, 9.04f, 2.98f)
            reflectiveQuadTo(9.93f, 2.8f)
            horizontalLineToRelative(4.13f)
            quadToRelative(0.46f, 0f, 0.89f, 0.18f)
            reflectiveQuadTo(15.7f, 3.49f)
            lineTo(16.96f, 4.8f)
            horizontalLineToRelative(2.97f)
            quadToRelative(0.94f, 0f, 1.61f, 0.67f)
            reflectiveQuadTo(22.2f, 7.07f)
            verticalLineTo(18.93f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(19.93f, 21.2f)
            horizontalLineTo(4.07f)
            close()
            moveToRelative(0f, -2.28f)
            horizontalLineTo(19.93f)
            verticalLineTo(7.07f)
            horizontalLineTo(15.97f)
            lineToRelative(-1.89f, -2f)
            horizontalLineTo(9.94f)
            lineToRelative(-1.93f, 2f)
            horizontalLineTo(4.07f)
            verticalLineTo(18.93f)
            close()
            moveTo(12f, 13f)
            close()
          }
        }
        .build().also { _photoCameraW500 = it }
    }

private var _photoCameraW300: ImageVector? = null
private var _photoCameraW400: ImageVector? = null
private var _photoCameraW500: ImageVector? = null

public val FujiIcons.Refresh: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _refreshW300 ?: ImageVector.Builder(
          name = "refresh",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.05f, 19.32f)
            quadToRelative(-3.07f, 0f, -5.21f, -2.13f)
            reflectiveQuadTo(4.71f, 12f)
            reflectiveQuadTo(6.85f, 6.81f)
            reflectiveQuadTo(12.05f, 4.68f)
            quadToRelative(1.74f, 0f, 3.29f, 0.78f)
            reflectiveQuadToRelative(2.56f, 2.2f)
            verticalLineTo(5.38f)
            quadToRelative(0f, -0.29f, 0.2f, -0.49f)
            reflectiveQuadTo(18.59f, 4.68f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            verticalLineTo(9.72f)
            quadToRelative(0f, 0.36f, -0.25f, 0.61f)
            reflectiveQuadToRelative(-0.61f, 0.25f)
            horizontalLineTo(14.1f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.21f)
            reflectiveQuadTo(13.4f, 9.88f)
            reflectiveQuadTo(13.6f, 9.38f)
            reflectiveQuadTo(14.1f, 9.18f)
            horizontalLineToRelative(3.15f)
            quadTo(16.47f, 7.74f, 15.08f, 6.91f)
            reflectiveQuadTo(12.05f, 6.08f)
            quadToRelative(-2.47f, 0f, -4.21f, 1.73f)
            reflectiveQuadTo(6.11f, 12f)
            reflectiveQuadToRelative(1.73f, 4.19f)
            reflectiveQuadToRelative(4.21f, 1.73f)
            quadToRelative(1.72f, 0f, 3.16f, -0.91f)
            reflectiveQuadToRelative(2.16f, -2.43f)
            quadToRelative(0.13f, -0.26f, 0.39f, -0.36f)
            reflectiveQuadToRelative(0.53f, -0.01f)
            quadToRelative(0.29f, 0.1f, 0.4f, 0.37f)
            reflectiveQuadToRelative(-0.01f, 0.53f)
            quadTo(17.78f, 17.03f, 16f, 18.17f)
            reflectiveQuadToRelative(-3.95f, 1.15f)
            close()
          }
        }
        .build().also { _refreshW300 = it }
        IconWeight.W400 -> _refreshW400 ?: ImageVector.Builder(
          name = "refresh",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 20f)
            quadTo(8.65f, 20f, 6.33f, 17.68f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadTo(6.33f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(1.73f, 0f, 3.3f, 0.71f)
            quadTo(16.88f, 5.43f, 18f, 6.75f)
            verticalLineTo(5f)
            quadTo(18f, 4.57f, 18.29f, 4.29f)
            reflectiveQuadTo(19f, 4f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(20f, 5f)
            verticalLineToRelative(5f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(19f, 11f)
            horizontalLineTo(14f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(13f, 10.43f, 13f, 10f)
            quadTo(13f, 9.57f, 13.29f, 9.29f)
            reflectiveQuadTo(14f, 9f)
            horizontalLineToRelative(3.2f)
            quadTo(16.4f, 7.6f, 15.01f, 6.8f)
            reflectiveQuadTo(12f, 6f)
            quadTo(9.5f, 6f, 7.75f, 7.75f)
            reflectiveQuadTo(6f, 12f)
            reflectiveQuadToRelative(1.75f, 4.25f)
            reflectiveQuadTo(12f, 18f)
            quadToRelative(1.7f, 0f, 3.11f, -0.86f)
            quadToRelative(1.41f, -0.86f, 2.19f, -2.31f)
            quadToRelative(0.2f, -0.35f, 0.56f, -0.49f)
            reflectiveQuadTo(18.6f, 14.33f)
            quadToRelative(0.4f, 0.13f, 0.57f, 0.53f)
            reflectiveQuadTo(19.15f, 15.6f)
            quadToRelative(-1.03f, 2f, -2.93f, 3.2f)
            reflectiveQuadTo(12f, 20f)
            close()
          }
        }
        .build().also { _refreshW400 = it }
        IconWeight.W500 -> _refreshW500 ?: ImageVector.Builder(
          name = "refresh",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.98f, 20.2f)
            quadToRelative(-3.43f, 0f, -5.82f, -2.39f)
            reflectiveQuadTo(3.77f, 12f)
            reflectiveQuadTo(6.16f, 6.18f)
            reflectiveQuadTo(11.98f, 3.8f)
            quadToRelative(1.78f, 0f, 3.39f, 0.73f)
            reflectiveQuadToRelative(2.76f, 2.1f)
            verticalLineTo(4.84f)
            quadToRelative(0f, -0.44f, 0.3f, -0.75f)
            reflectiveQuadTo(19.18f, 3.8f)
            reflectiveQuadToRelative(0.75f, 0.3f)
            reflectiveQuadToRelative(0.3f, 0.75f)
            verticalLineTo(9.95f)
            quadToRelative(0f, 0.48f, -0.33f, 0.81f)
            reflectiveQuadToRelative(-0.81f, 0.33f)
            horizontalLineTo(13.97f)
            quadToRelative(-0.44f, 0f, -0.74f, -0.3f)
            reflectiveQuadToRelative(-0.3f, -0.75f)
            reflectiveQuadTo(13.23f, 9.3f)
            reflectiveQuadTo(13.98f, 9f)
            horizontalLineToRelative(3.12f)
            quadTo(16.29f, 7.64f, 14.93f, 6.85f)
            reflectiveQuadTo(11.98f, 6.07f)
            quadToRelative(-2.47f, 0f, -4.2f, 1.73f)
            reflectiveQuadTo(6.05f, 12f)
            reflectiveQuadToRelative(1.73f, 4.2f)
            reflectiveQuadToRelative(4.2f, 1.73f)
            quadToRelative(1.63f, 0f, 3f, -0.82f)
            reflectiveQuadToRelative(2.15f, -2.19f)
            quadToRelative(0.24f, -0.39f, 0.66f, -0.54f)
            reflectiveQuadToRelative(0.85f, -0f)
            quadToRelative(0.45f, 0.15f, 0.65f, 0.58f)
            reflectiveQuadToRelative(-0.03f, 0.82f)
            quadTo(18.19f, 17.79f, 16.26f, 19f)
            reflectiveQuadTo(11.98f, 20.2f)
            close()
          }
        }
        .build().also { _refreshW500 = it }
    }

private var _refreshW300: ImageVector? = null
private var _refreshW400: ImageVector? = null
private var _refreshW500: ImageVector? = null

public val FujiIcons.Remove: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _removeW300 ?: ImageVector.Builder(
          name = "remove",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6.35f, 12.64f)
            quadToRelative(-0.28f, 0f, -0.46f, -0.19f)
            reflectiveQuadTo(5.7f, 11.99f)
            reflectiveQuadTo(5.89f, 11.52f)
            reflectiveQuadTo(6.35f, 11.34f)
            horizontalLineToRelative(11.3f)
            quadToRelative(0.28f, 0f, 0.46f, 0.19f)
            reflectiveQuadToRelative(0.19f, 0.47f)
            reflectiveQuadToRelative(-0.19f, 0.46f)
            reflectiveQuadToRelative(-0.46f, 0.18f)
            horizontalLineTo(6.35f)
            close()
          }
        }
        .build().also { _removeW300 = it }
        IconWeight.W400 -> _removeW400 ?: ImageVector.Builder(
          name = "remove",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6f, 13f)
            quadTo(5.58f, 13f, 5.29f, 12.71f)
            quadTo(5f, 12.43f, 5f, 12f)
            reflectiveQuadTo(5.29f, 11.29f)
            reflectiveQuadTo(6f, 11f)
            horizontalLineTo(18f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(19f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(18f, 13f)
            horizontalLineTo(6f)
            close()
          }
        }
        .build().also { _removeW400 = it }
        IconWeight.W500 -> _removeW500 ?: ImageVector.Builder(
          name = "remove",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.93f, 13.14f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(4.8f, 12f)
            reflectiveQuadTo(5.13f, 11.19f)
            reflectiveQuadTo(5.93f, 10.86f)
            horizontalLineTo(18.07f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(19.2f, 12f)
            reflectiveQuadToRelative(-0.33f, 0.81f)
            reflectiveQuadToRelative(-0.81f, 0.33f)
            horizontalLineTo(5.93f)
            close()
          }
        }
        .build().also { _removeW500 = it }
    }

private var _removeW300: ImageVector? = null
private var _removeW400: ImageVector? = null
private var _removeW500: ImageVector? = null

public val FujiIcons.Schedule: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _scheduleW300 ?: ImageVector.Builder(
          name = "schedule",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.71f, 11.72f)
            verticalLineTo(7.76f)
            quadToRelative(0f, -0.29f, -0.21f, -0.49f)
            reflectiveQuadTo(12.01f, 7.06f)
            reflectiveQuadTo(11.52f, 7.27f)
            reflectiveQuadTo(11.31f, 7.76f)
            verticalLineToRelative(4.18f)
            quadToRelative(0f, 0.16f, 0.06f, 0.32f)
            reflectiveQuadToRelative(0.19f, 0.28f)
            lineToRelative(3.4f, 3.41f)
            quadToRelative(0.2f, 0.21f, 0.49f, 0.21f)
            reflectiveQuadToRelative(0.5f, -0.21f)
            quadToRelative(0.21f, -0.21f, 0.21f, -0.49f)
            reflectiveQuadToRelative(-0.21f, -0.5f)
            lineTo(12.71f, 11.72f)
            close()
            moveTo(12f, 21.3f)
            quadToRelative(-1.93f, 0f, -3.63f, -0.73f)
            reflectiveQuadTo(5.42f, 18.58f)
            reflectiveQuadTo(3.43f, 15.62f)
            reflectiveQuadTo(2.7f, 12f)
            quadToRelative(0f, -1.93f, 0.73f, -3.63f)
            reflectiveQuadTo(5.42f, 5.42f)
            reflectiveQuadTo(8.38f, 3.43f)
            reflectiveQuadTo(12f, 2.7f)
            quadToRelative(1.93f, 0f, 3.63f, 0.73f)
            reflectiveQuadToRelative(2.95f, 1.99f)
            reflectiveQuadToRelative(1.99f, 2.95f)
            reflectiveQuadTo(21.3f, 12f)
            quadToRelative(0f, 1.93f, -0.73f, 3.63f)
            reflectiveQuadToRelative(-1.99f, 2.95f)
            reflectiveQuadToRelative(-2.95f, 1.99f)
            reflectiveQuadTo(12f, 21.3f)
            close()
            moveTo(12f, 12f)
            close()
            moveToRelative(0f, 7.9f)
            quadToRelative(3.28f, 0f, 5.59f, -2.31f)
            reflectiveQuadTo(19.9f, 12f)
            reflectiveQuadTo(17.59f, 6.41f)
            reflectiveQuadTo(12f, 4.1f)
            reflectiveQuadTo(6.41f, 6.41f)
            reflectiveQuadTo(4.1f, 12f)
            reflectiveQuadToRelative(2.31f, 5.59f)
            reflectiveQuadTo(12f, 19.9f)
            close()
          }
        }
        .build().also { _scheduleW300 = it }
        IconWeight.W400 -> _scheduleW400 ?: ImageVector.Builder(
          name = "schedule",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(13f, 11.6f)
            verticalLineTo(8f)
            quadTo(13f, 7.57f, 12.71f, 7.29f)
            reflectiveQuadTo(12f, 7f)
            reflectiveQuadTo(11.29f, 7.29f)
            reflectiveQuadTo(11f, 8f)
            verticalLineToRelative(3.97f)
            quadToRelative(0f, 0.2f, 0.08f, 0.39f)
            reflectiveQuadTo(11.3f, 12.7f)
            lineTo(14.6f, 16f)
            quadToRelative(0.27f, 0.27f, 0.7f, 0.27f)
            reflectiveQuadTo(16f, 16f)
            quadToRelative(0.28f, -0.28f, 0.28f, -0.7f)
            quadToRelative(0f, -0.42f, -0.28f, -0.7f)
            lineToRelative(-3f, -3f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveTo(12f, 12f)
            close()
            moveToRelative(0f, 8f)
            quadToRelative(3.33f, 0f, 5.66f, -2.34f)
            reflectiveQuadTo(20f, 12f)
            quadTo(20f, 8.67f, 17.66f, 6.34f)
            reflectiveQuadTo(12f, 4f)
            quadTo(8.68f, 4f, 6.34f, 6.34f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(2.34f, 5.66f)
            reflectiveQuadTo(12f, 20f)
            close()
          }
        }
        .build().also { _scheduleW400 = it }
        IconWeight.W500 -> _scheduleW500 ?: ImageVector.Builder(
          name = "schedule",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(13.09f, 11.55f)
            verticalLineTo(8.12f)
            quadToRelative(0f, -0.46f, -0.31f, -0.78f)
            reflectiveQuadTo(12f, 7.03f)
            reflectiveQuadTo(11.22f, 7.34f)
            reflectiveQuadTo(10.91f, 8.12f)
            verticalLineToRelative(3.84f)
            quadToRelative(0f, 0.23f, 0.08f, 0.44f)
            reflectiveQuadToRelative(0.25f, 0.38f)
            lineToRelative(3.2f, 3.2f)
            quadToRelative(0.3f, 0.3f, 0.76f, 0.3f)
            reflectiveQuadToRelative(0.77f, -0.3f)
            reflectiveQuadToRelative(0.31f, -0.77f)
            reflectiveQuadTo(15.98f, 14.44f)
            lineTo(13.09f, 11.55f)
            close()
            moveTo(12f, 22.2f)
            quadToRelative(-2.12f, 0f, -3.98f, -0.8f)
            reflectiveQuadTo(4.78f, 19.22f)
            reflectiveQuadTo(2.6f, 15.98f)
            reflectiveQuadTo(1.8f, 12f)
            reflectiveQuadTo(2.6f, 8.02f)
            reflectiveQuadTo(4.78f, 4.78f)
            reflectiveQuadTo(8.02f, 2.6f)
            reflectiveQuadTo(12f, 1.8f)
            reflectiveQuadToRelative(3.98f, 0.8f)
            reflectiveQuadToRelative(3.24f, 2.18f)
            reflectiveQuadTo(21.4f, 8.02f)
            reflectiveQuadTo(22.2f, 12f)
            reflectiveQuadToRelative(-0.8f, 3.98f)
            reflectiveQuadToRelative(-2.18f, 3.24f)
            reflectiveQuadTo(15.98f, 21.4f)
            reflectiveQuadTo(12f, 22.2f)
            close()
            moveTo(12f, 12f)
            close()
            moveToRelative(-0f, 7.93f)
            quadToRelative(3.3f, 0f, 5.61f, -2.31f)
            reflectiveQuadTo(19.93f, 12f)
            reflectiveQuadTo(17.61f, 6.39f)
            reflectiveQuadTo(12f, 4.07f)
            reflectiveQuadTo(6.39f, 6.39f)
            reflectiveQuadTo(4.07f, 12f)
            reflectiveQuadToRelative(2.32f, 5.61f)
            reflectiveQuadTo(12f, 19.93f)
            close()
          }
        }
        .build().also { _scheduleW500 = it }
    }

private var _scheduleW300: ImageVector? = null
private var _scheduleW400: ImageVector? = null
private var _scheduleW500: ImageVector? = null

public val FujiIcons.Search: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _searchW300 ?: ImageVector.Builder(
          name = "search",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.56f, 15.49f)
            quadToRelative(-2.51f, 0f, -4.24f, -1.74f)
            reflectiveQuadTo(3.58f, 9.52f)
            reflectiveQuadTo(5.31f, 5.28f)
            reflectiveQuadTo(9.55f, 3.55f)
            reflectiveQuadToRelative(4.24f, 1.74f)
            reflectiveQuadToRelative(1.74f, 4.24f)
            quadToRelative(0f, 1.05f, -0.35f, 2.02f)
            reflectiveQuadToRelative(-0.94f, 1.68f)
            lineToRelative(5.71f, 5.7f)
            quadToRelative(0.2f, 0.2f, 0.2f, 0.49f)
            reflectiveQuadToRelative(-0.21f, 0.5f)
            reflectiveQuadToRelative(-0.5f, 0.21f)
            reflectiveQuadTo(18.95f, 19.91f)
            lineTo(13.25f, 14.2f)
            quadToRelative(-0.74f, 0.62f, -1.71f, 0.96f)
            reflectiveQuadTo(9.56f, 15.49f)
            close()
            moveToRelative(-0f, -1.4f)
            quadToRelative(1.92f, 0f, 3.25f, -1.33f)
            reflectiveQuadTo(14.13f, 9.52f)
            reflectiveQuadTo(12.8f, 6.27f)
            reflectiveQuadTo(9.55f, 4.95f)
            reflectiveQuadTo(6.3f, 6.27f)
            reflectiveQuadTo(4.98f, 9.52f)
            reflectiveQuadTo(6.3f, 12.77f)
            reflectiveQuadTo(9.55f, 14.1f)
            close()
          }
        }
        .build().also { _searchW300 = it }
        IconWeight.W400 -> _searchW400 ?: ImageVector.Builder(
          name = "search",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.5f, 16f)
            quadTo(6.78f, 16f, 4.89f, 14.11f)
            quadTo(3f, 12.23f, 3f, 9.5f)
            quadTo(3f, 6.77f, 4.89f, 4.89f)
            reflectiveQuadTo(9.5f, 3f)
            reflectiveQuadToRelative(4.61f, 1.89f)
            reflectiveQuadTo(16f, 9.5f)
            quadToRelative(0f, 1.1f, -0.35f, 2.07f)
            reflectiveQuadTo(14.7f, 13.3f)
            lineToRelative(5.6f, 5.6f)
            quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
            quadToRelative(0f, 0.42f, -0.28f, 0.7f)
            quadToRelative(-0.27f, 0.27f, -0.7f, 0.27f)
            reflectiveQuadTo(18.9f, 20.3f)
            lineTo(13.3f, 14.7f)
            quadToRelative(-0.75f, 0.6f, -1.72f, 0.95f)
            reflectiveQuadTo(9.5f, 16f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(1.88f, 0f, 3.19f, -1.31f)
            reflectiveQuadTo(14f, 9.5f)
            reflectiveQuadTo(12.69f, 6.31f)
            reflectiveQuadTo(9.5f, 5f)
            reflectiveQuadTo(6.31f, 6.31f)
            reflectiveQuadTo(5f, 9.5f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(9.5f, 14f)
            close()
          }
        }
        .build().also { _searchW400 = it }
        IconWeight.W500 -> _searchW500 ?: ImageVector.Builder(
          name = "search",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.45f, 16.14f)
            quadToRelative(-2.78f, 0f, -4.71f, -1.93f)
            reflectiveQuadTo(2.81f, 9.5f)
            reflectiveQuadTo(4.74f, 4.79f)
            reflectiveQuadTo(9.45f, 2.86f)
            reflectiveQuadToRelative(4.71f, 1.93f)
            reflectiveQuadTo(16.09f, 9.5f)
            quadToRelative(0f, 1.11f, -0.34f, 2.08f)
            reflectiveQuadToRelative(-0.91f, 1.7f)
            lineToRelative(5.55f, 5.56f)
            quadToRelative(0.32f, 0.32f, 0.32f, 0.8f)
            reflectiveQuadToRelative(-0.32f, 0.79f)
            quadToRelative(-0.32f, 0.32f, -0.8f, 0.32f)
            reflectiveQuadToRelative(-0.8f, -0.32f)
            lineTo(13.25f, 14.89f)
            quadTo(12.5f, 15.46f, 11.53f, 15.8f)
            reflectiveQuadTo(9.45f, 16.14f)
            close()
            moveToRelative(0f, -2.28f)
            quadToRelative(1.82f, 0f, 3.09f, -1.27f)
            reflectiveQuadTo(13.81f, 9.5f)
            reflectiveQuadTo(12.54f, 6.41f)
            reflectiveQuadTo(9.45f, 5.14f)
            reflectiveQuadTo(6.36f, 6.41f)
            reflectiveQuadTo(5.09f, 9.5f)
            reflectiveQuadToRelative(1.27f, 3.09f)
            reflectiveQuadToRelative(3.09f, 1.27f)
            close()
          }
        }
        .build().also { _searchW500 = it }
    }

private var _searchW300: ImageVector? = null
private var _searchW400: ImageVector? = null
private var _searchW500: ImageVector? = null

public val FujiIcons.Settings: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _settingsW300 ?: ImageVector.Builder(
          name = "settings",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.77f, 21.3f)
            quadToRelative(-0.41f, 0f, -0.72f, -0.27f)
            reflectiveQuadTo(9.69f, 20.35f)
            lineTo(9.43f, 18.32f)
            quadTo(9.02f, 18.18f, 8.56f, 17.93f)
            reflectiveQuadTo(7.77f, 17.4f)
            lineTo(5.91f, 18.18f)
            quadTo(5.53f, 18.35f, 5.15f, 18.21f)
            reflectiveQuadTo(4.55f, 17.73f)
            lineTo(3.29f, 15.56f)
            quadTo(3.07f, 15.2f, 3.16f, 14.8f)
            reflectiveQuadTo(3.58f, 14.14f)
            lineTo(5.22f, 12.91f)
            quadTo(5.18f, 12.69f, 5.17f, 12.48f)
            reflectiveQuadTo(5.15f, 12.01f)
            quadToRelative(0f, -0.19f, 0.02f, -0.42f)
            reflectiveQuadTo(5.23f, 11.05f)
            lineTo(3.6f, 9.85f)
            quadTo(3.27f, 9.59f, 3.18f, 9.19f)
            reflectiveQuadTo(3.31f, 8.42f)
            lineTo(4.55f, 6.29f)
            quadTo(4.77f, 5.95f, 5.15f, 5.81f)
            reflectiveQuadTo(5.92f, 5.83f)
            lineTo(7.8f, 6.62f)
            quadTo(8.14f, 6.34f, 8.55f, 6.1f)
            reflectiveQuadTo(9.42f, 5.7f)
            lineTo(9.69f, 3.63f)
            quadTo(9.75f, 3.23f, 10.06f, 2.96f)
            reflectiveQuadTo(10.78f, 2.68f)
            horizontalLineToRelative(2.45f)
            quadToRelative(0.41f, 0f, 0.72f, 0.27f)
            reflectiveQuadToRelative(0.37f, 0.68f)
            lineToRelative(0.26f, 2.04f)
            quadToRelative(0.47f, 0.17f, 0.84f, 0.4f)
            reflectiveQuadToRelative(0.74f, 0.55f)
            lineTo(18.1f, 5.83f)
            quadTo(18.48f, 5.67f, 18.86f, 5.81f)
            reflectiveQuadToRelative(0.6f, 0.48f)
            lineTo(20.7f, 8.43f)
            quadToRelative(0.22f, 0.36f, 0.12f, 0.76f)
            reflectiveQuadTo(20.41f, 9.85f)
            lineToRelative(-1.72f, 1.28f)
            quadToRelative(0.05f, 0.24f, 0.06f, 0.44f)
            reflectiveQuadTo(18.75f, 12f)
            quadToRelative(0f, 0.23f, -0.01f, 0.44f)
            reflectiveQuadTo(18.68f, 12.9f)
            lineToRelative(1.67f, 1.24f)
            quadToRelative(0.33f, 0.26f, 0.43f, 0.66f)
            reflectiveQuadToRelative(-0.12f, 0.75f)
            lineToRelative(-1.24f, 2.16f)
            quadToRelative(-0.22f, 0.35f, -0.61f, 0.49f)
            reflectiveQuadTo(18.03f, 18.17f)
            lineToRelative(-1.87f, -0.8f)
            quadToRelative(-0.34f, 0.29f, -0.71f, 0.51f)
            reflectiveQuadTo(14.58f, 18.3f)
            lineToRelative(-0.26f, 2.04f)
            quadToRelative(-0.06f, 0.4f, -0.37f, 0.68f)
            reflectiveQuadTo(13.23f, 21.3f)
            horizontalLineTo(10.77f)
            close()
            moveToRelative(0.24f, -1.4f)
            horizontalLineToRelative(1.94f)
            lineToRelative(0.37f, -2.69f)
            quadToRelative(0.75f, -0.2f, 1.35f, -0.55f)
            reflectiveQuadToRelative(1.18f, -0.93f)
            lineToRelative(2.5f, 1.06f)
            lineToRelative(0.97f, -1.68f)
            lineTo(17.13f, 13.48f)
            quadToRelative(0.11f, -0.39f, 0.16f, -0.75f)
            reflectiveQuadTo(17.33f, 12f)
            quadToRelative(0f, -0.38f, -0.04f, -0.72f)
            reflectiveQuadTo(17.13f, 10.54f)
            lineTo(19.33f, 8.87f)
            lineTo(18.38f, 7.19f)
            lineTo(15.83f, 8.27f)
            quadTo(15.37f, 7.77f, 14.67f, 7.36f)
            reflectiveQuadTo(13.31f, 6.79f)
            lineTo(12.99f, 4.08f)
            horizontalLineTo(11.02f)
            lineTo(10.7f, 6.77f)
            quadTo(9.91f, 6.96f, 9.31f, 7.3f)
            reflectiveQuadTo(8.1f, 8.25f)
            lineTo(5.62f, 7.19f)
            lineTo(4.65f, 8.87f)
            lineToRelative(2.15f, 1.6f)
            quadTo(6.67f, 10.84f, 6.62f, 11.22f)
            reflectiveQuadTo(6.57f, 12f)
            quadToRelative(0f, 0.38f, 0.04f, 0.74f)
            reflectiveQuadTo(6.78f, 13.5f)
            lineTo(4.65f, 15.11f)
            lineToRelative(0.98f, 1.68f)
            lineTo(8.08f, 15.74f)
            quadToRelative(0.6f, 0.6f, 1.22f, 0.95f)
            reflectiveQuadToRelative(1.39f, 0.54f)
            lineToRelative(0.32f, 2.67f)
            close()
            moveToRelative(0.96f, -5f)
            quadToRelative(1.2f, 0f, 2.05f, -0.85f)
            reflectiveQuadTo(14.86f, 12f)
            reflectiveQuadTo(14.02f, 9.95f)
            reflectiveQuadTo(11.96f, 9.1f)
            quadToRelative(-1.22f, 0f, -2.06f, 0.85f)
            reflectiveQuadTo(9.06f, 12f)
            reflectiveQuadToRelative(0.84f, 2.05f)
            reflectiveQuadToRelative(2.06f, 0.85f)
            close()
            moveToRelative(0.02f, -2.91f)
            close()
          }
        }
        .build().also { _settingsW300 = it }
        IconWeight.W400 -> _settingsW400 ?: ImageVector.Builder(
          name = "settings",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.83f, 22f)
            quadTo(10.15f, 22f, 9.66f, 21.55f)
            reflectiveQuadTo(9.08f, 20.45f)
            lineTo(8.85f, 18.8f)
            quadTo(8.53f, 18.68f, 8.24f, 18.5f)
            reflectiveQuadTo(7.68f, 18.13f)
            lineTo(6.13f, 18.77f)
            quadTo(5.5f, 19.05f, 4.88f, 18.83f)
            reflectiveQuadTo(3.9f, 18.02f)
            lineTo(2.73f, 15.98f)
            quadTo(2.38f, 15.4f, 2.53f, 14.75f)
            reflectiveQuadTo(3.2f, 13.68f)
            lineToRelative(1.33f, -1f)
            quadTo(4.5f, 12.5f, 4.5f, 12.34f)
            quadToRelative(0f, -0.16f, 0f, -0.34f)
            reflectiveQuadToRelative(0f, -0.34f)
            reflectiveQuadTo(4.53f, 11.33f)
            lineToRelative(-1.33f, -1f)
            quadTo(2.68f, 9.9f, 2.53f, 9.25f)
            reflectiveQuadTo(2.73f, 8.02f)
            lineTo(3.9f, 5.97f)
            quadTo(4.25f, 5.4f, 4.88f, 5.18f)
            reflectiveQuadTo(6.13f, 5.22f)
            lineTo(7.68f, 5.88f)
            quadTo(7.95f, 5.68f, 8.25f, 5.5f)
            reflectiveQuadTo(8.85f, 5.2f)
            lineTo(9.08f, 3.55f)
            quadTo(9.18f, 2.9f, 9.66f, 2.45f)
            reflectiveQuadTo(10.83f, 2f)
            horizontalLineToRelative(2.35f)
            quadToRelative(0.68f, 0f, 1.16f, 0.45f)
            reflectiveQuadToRelative(0.59f, 1.1f)
            lineTo(15.15f, 5.2f)
            quadToRelative(0.33f, 0.13f, 0.61f, 0.3f)
            reflectiveQuadToRelative(0.56f, 0.38f)
            lineTo(17.88f, 5.22f)
            quadTo(18.5f, 4.95f, 19.13f, 5.18f)
            reflectiveQuadToRelative(0.98f, 0.8f)
            lineToRelative(1.18f, 2.05f)
            quadToRelative(0.35f, 0.58f, 0.2f, 1.23f)
            reflectiveQuadTo(20.8f, 10.33f)
            lineToRelative(-1.32f, 1f)
            quadToRelative(0.02f, 0.18f, 0.02f, 0.34f)
            reflectiveQuadToRelative(0f, 0.34f)
            reflectiveQuadToRelative(0f, 0.34f)
            reflectiveQuadToRelative(-0.05f, 0.34f)
            lineToRelative(1.32f, 1f)
            quadToRelative(0.52f, 0.43f, 0.68f, 1.08f)
            reflectiveQuadToRelative(-0.2f, 1.22f)
            lineToRelative(-1.2f, 2.05f)
            quadToRelative(-0.35f, 0.58f, -0.98f, 0.8f)
            reflectiveQuadTo(17.83f, 18.77f)
            lineToRelative(-1.5f, -0.65f)
            quadToRelative(-0.27f, 0.2f, -0.57f, 0.38f)
            reflectiveQuadToRelative(-0.6f, 0.3f)
            lineToRelative(-0.22f, 1.65f)
            quadToRelative(-0.1f, 0.65f, -0.59f, 1.1f)
            reflectiveQuadTo(13.18f, 22f)
            horizontalLineTo(10.83f)
            close()
            moveTo(11f, 20f)
            horizontalLineToRelative(1.98f)
            lineToRelative(0.35f, -2.65f)
            quadToRelative(0.78f, -0.2f, 1.44f, -0.59f)
            reflectiveQuadToRelative(1.21f, -0.94f)
            lineToRelative(2.47f, 1.03f)
            lineToRelative(0.98f, -1.7f)
            lineTo(17.28f, 13.52f)
            quadToRelative(0.13f, -0.35f, 0.17f, -0.74f)
            reflectiveQuadTo(17.5f, 12f)
            reflectiveQuadTo(17.45f, 11.21f)
            quadTo(17.4f, 10.83f, 17.28f, 10.48f)
            lineTo(19.43f, 8.85f)
            lineTo(18.45f, 7.15f)
            lineTo(15.98f, 8.2f)
            quadTo(15.43f, 7.63f, 14.76f, 7.24f)
            reflectiveQuadTo(13.33f, 6.65f)
            lineTo(13f, 4f)
            horizontalLineTo(11.03f)
            lineTo(10.68f, 6.65f)
            quadTo(9.9f, 6.85f, 9.24f, 7.24f)
            reflectiveQuadTo(8.03f, 8.17f)
            lineTo(5.55f, 7.15f)
            lineTo(4.58f, 8.85f)
            lineToRelative(2.15f, 1.6f)
            quadTo(6.6f, 10.83f, 6.55f, 11.2f)
            reflectiveQuadTo(6.5f, 12f)
            quadToRelative(0f, 0.4f, 0.05f, 0.77f)
            reflectiveQuadToRelative(0.17f, 0.75f)
            lineTo(4.58f, 15.15f)
            lineToRelative(0.98f, 1.7f)
            lineTo(8.03f, 15.8f)
            quadToRelative(0.55f, 0.58f, 1.21f, 0.96f)
            reflectiveQuadToRelative(1.44f, 0.59f)
            lineTo(11f, 20f)
            close()
            moveToRelative(1.05f, -4.5f)
            quadToRelative(1.45f, 0f, 2.47f, -1.03f)
            reflectiveQuadTo(15.55f, 12f)
            reflectiveQuadTo(14.53f, 9.52f)
            reflectiveQuadTo(12.05f, 8.5f)
            quadToRelative(-1.47f, 0f, -2.49f, 1.02f)
            reflectiveQuadTo(8.55f, 12f)
            reflectiveQuadToRelative(1.01f, 2.47f)
            reflectiveQuadToRelative(2.49f, 1.03f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build().also { _settingsW400 = it }
        IconWeight.W500 -> _settingsW500 ?: ImageVector.Builder(
          name = "settings",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.71f, 22.2f)
            quadToRelative(-0.7f, 0f, -1.21f, -0.46f)
            reflectiveQuadTo(8.89f, 20.59f)
            lineTo(8.66f, 18.94f)
            quadTo(8.39f, 18.83f, 8.14f, 18.68f)
            reflectiveQuadTo(7.65f, 18.36f)
            lineTo(6.11f, 19.01f)
            quadTo(5.46f, 19.29f, 4.8f, 19.06f)
            reflectiveQuadTo(3.79f, 18.23f)
            lineTo(2.5f, 15.97f)
            quadTo(2.14f, 15.37f, 2.29f, 14.69f)
            reflectiveQuadToRelative(0.7f, -1.11f)
            lineToRelative(1.32f, -1f)
            quadTo(4.29f, 12.43f, 4.29f, 12.29f)
            reflectiveQuadTo(4.29f, 12f)
            reflectiveQuadToRelative(0f, -0.29f)
            reflectiveQuadTo(4.31f, 11.42f)
            lineTo(2.99f, 10.43f)
            quadTo(2.44f, 9.99f, 2.29f, 9.31f)
            reflectiveQuadTo(2.5f, 8.03f)
            lineTo(3.79f, 5.78f)
            quadTo(4.15f, 5.18f, 4.8f, 4.95f)
            reflectiveQuadTo(6.1f, 5f)
            lineTo(7.66f, 5.65f)
            quadTo(7.9f, 5.48f, 8.15f, 5.33f)
            reflectiveQuadTo(8.66f, 5.07f)
            lineTo(8.89f, 3.41f)
            quadTo(8.99f, 2.73f, 9.5f, 2.26f)
            reflectiveQuadTo(10.71f, 1.8f)
            horizontalLineToRelative(2.58f)
            quadToRelative(0.7f, 0f, 1.21f, 0.46f)
            reflectiveQuadToRelative(0.61f, 1.15f)
            lineToRelative(0.22f, 1.66f)
            quadToRelative(0.28f, 0.11f, 0.53f, 0.26f)
            reflectiveQuadToRelative(0.49f, 0.32f)
            lineTo(17.89f, 5f)
            quadTo(18.54f, 4.72f, 19.2f, 4.95f)
            reflectiveQuadToRelative(1.02f, 0.83f)
            lineTo(21.5f, 8.03f)
            quadToRelative(0.36f, 0.6f, 0.21f, 1.28f)
            reflectiveQuadToRelative(-0.7f, 1.11f)
            lineToRelative(-1.32f, 0.99f)
            quadToRelative(0.02f, 0.15f, 0.02f, 0.29f)
            reflectiveQuadToRelative(0f, 0.29f)
            reflectiveQuadToRelative(-0f, 0.29f)
            reflectiveQuadToRelative(-0.04f, 0.29f)
            lineToRelative(1.32f, 0.99f)
            quadToRelative(0.55f, 0.44f, 0.7f, 1.11f)
            reflectiveQuadToRelative(-0.21f, 1.28f)
            lineToRelative(-1.31f, 2.26f)
            quadToRelative(-0.36f, 0.6f, -1.01f, 0.83f)
            reflectiveQuadToRelative(-1.3f, -0.05f)
            lineTo(16.34f, 18.36f)
            quadToRelative(-0.24f, 0.17f, -0.49f, 0.32f)
            reflectiveQuadToRelative(-0.51f, 0.26f)
            lineToRelative(-0.22f, 1.66f)
            quadToRelative(-0.1f, 0.68f, -0.61f, 1.14f)
            reflectiveQuadTo(13.29f, 22.2f)
            horizontalLineTo(10.71f)
            close()
            moveToRelative(0.35f, -2.28f)
            horizontalLineTo(12.9f)
            lineToRelative(0.36f, -2.64f)
            quadToRelative(0.78f, -0.2f, 1.46f, -0.59f)
            reflectiveQuadToRelative(1.23f, -0.96f)
            lineToRelative(2.47f, 1.03f)
            lineToRelative(0.9f, -1.59f)
            lineTo(17.19f, 13.55f)
            quadToRelative(0.13f, -0.36f, 0.18f, -0.76f)
            reflectiveQuadTo(17.42f, 12f)
            reflectiveQuadTo(17.37f, 11.2f)
            reflectiveQuadTo(17.19f, 10.45f)
            lineTo(19.33f, 8.83f)
            lineTo(18.41f, 7.24f)
            lineTo(15.95f, 8.29f)
            quadTo(15.4f, 7.7f, 14.72f, 7.3f)
            reflectiveQuadTo(13.26f, 6.71f)
            lineTo(12.93f, 4.07f)
            horizontalLineTo(11.08f)
            lineTo(10.75f, 6.7f)
            quadTo(9.95f, 6.9f, 9.27f, 7.29f)
            reflectiveQuadTo(8.04f, 8.26f)
            lineTo(5.58f, 7.24f)
            lineTo(4.67f, 8.83f)
            lineTo(6.8f, 10.41f)
            quadTo(6.67f, 10.8f, 6.62f, 11.19f)
            reflectiveQuadTo(6.56f, 12f)
            quadToRelative(0f, 0.41f, 0.06f, 0.79f)
            reflectiveQuadTo(6.8f, 13.57f)
            lineToRelative(-2.13f, 1.6f)
            lineToRelative(0.91f, 1.59f)
            lineTo(8.04f, 15.72f)
            quadTo(8.59f, 16.3f, 9.28f, 16.7f)
            reflectiveQuadToRelative(1.47f, 0.6f)
            lineToRelative(0.32f, 2.63f)
            close()
            moveTo(12.03f, 15.5f)
            quadToRelative(1.45f, 0f, 2.47f, -1.03f)
            reflectiveQuadTo(15.53f, 12f)
            reflectiveQuadTo(14.51f, 9.52f)
            reflectiveQuadTo(12.03f, 8.5f)
            quadToRelative(-1.47f, 0f, -2.48f, 1.02f)
            reflectiveQuadTo(8.53f, 12f)
            reflectiveQuadToRelative(1.02f, 2.47f)
            reflectiveQuadToRelative(2.48f, 1.03f)
            close()
            moveTo(12f, 12f)
            close()
          }
        }
        .build().also { _settingsW500 = it }
    }

private var _settingsW300: ImageVector? = null
private var _settingsW400: ImageVector? = null
private var _settingsW500: ImageVector? = null

public val FujiIcons.Sort: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _sortW300 ?: ImageVector.Builder(
          name = "sort",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.4f, 17.3f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(3.7f, 16.6f)
            reflectiveQuadTo(3.91f, 16.1f)
            reflectiveQuadTo(4.4f, 15.9f)
            horizontalLineToRelative(3.8f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadTo(8.91f, 16.6f)
            reflectiveQuadTo(8.7f, 17.09f)
            reflectiveQuadTo(8.21f, 17.3f)
            horizontalLineTo(4.4f)
            close()
            moveToRelative(0f, -4.5f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.2f)
            reflectiveQuadTo(3.7f, 12.1f)
            reflectiveQuadTo(3.91f, 11.6f)
            reflectiveQuadTo(4.4f, 11.4f)
            horizontalLineToRelative(9.5f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.5f)
            reflectiveQuadTo(14.4f, 12.6f)
            reflectiveQuadTo(13.9f, 12.8f)
            horizontalLineTo(4.4f)
            close()
            moveToRelative(0f, -4.5f)
            quadTo(4.11f, 8.3f, 3.91f, 8.1f)
            reflectiveQuadTo(3.7f, 7.6f)
            reflectiveQuadTo(3.91f, 7.11f)
            reflectiveQuadTo(4.4f, 6.9f)
            horizontalLineTo(19.6f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadTo(20.3f, 7.6f)
            reflectiveQuadTo(20.09f, 8.1f)
            reflectiveQuadTo(19.6f, 8.3f)
            horizontalLineTo(4.4f)
            close()
          }
        }
        .build().also { _sortW300 = it }
        IconWeight.W400 -> _sortW400 ?: ImageVector.Builder(
          name = "sort",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4f, 18f)
            quadTo(3.58f, 18f, 3.29f, 17.71f)
            quadTo(3f, 17.43f, 3f, 17f)
            reflectiveQuadTo(3.29f, 16.29f)
            reflectiveQuadTo(4f, 16f)
            horizontalLineTo(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(9f, 17f)
            reflectiveQuadTo(8.71f, 17.71f)
            reflectiveQuadTo(8f, 18f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 13f)
            quadTo(3.58f, 13f, 3.29f, 12.71f)
            quadTo(3f, 12.43f, 3f, 12f)
            reflectiveQuadTo(3.29f, 11.29f)
            reflectiveQuadTo(4f, 11f)
            horizontalLineTo(14f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(15f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(14f, 13f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 8f)
            quadTo(3.58f, 8f, 3.29f, 7.71f)
            quadTo(3f, 7.43f, 3f, 7f)
            reflectiveQuadTo(3.29f, 6.29f)
            reflectiveQuadTo(4f, 6f)
            horizontalLineTo(20f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 7f)
            reflectiveQuadTo(20.71f, 7.71f)
            reflectiveQuadTo(20f, 8f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build().also { _sortW400 = it }
        IconWeight.W500 -> _sortW500 ?: ImageVector.Builder(
          name = "sort",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(3.93f, 18.41f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(2.8f, 17.27f)
            reflectiveQuadTo(3.12f, 16.46f)
            reflectiveQuadTo(3.93f, 16.14f)
            horizontalLineTo(7.97f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(9.1f, 17.28f)
            reflectiveQuadTo(8.78f, 18.08f)
            reflectiveQuadTo(7.97f, 18.41f)
            horizontalLineTo(3.93f)
            close()
            moveToRelative(0f, -5.27f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(2.8f, 12f)
            reflectiveQuadTo(3.12f, 11.19f)
            reflectiveQuadTo(3.93f, 10.87f)
            horizontalLineTo(14.02f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadToRelative(0.33f, 0.81f)
            reflectiveQuadToRelative(-0.33f, 0.81f)
            reflectiveQuadToRelative(-0.81f, 0.32f)
            horizontalLineTo(3.93f)
            close()
            moveToRelative(0f, -5.27f)
            quadTo(3.45f, 7.87f, 3.12f, 7.54f)
            reflectiveQuadTo(2.8f, 6.73f)
            reflectiveQuadTo(3.13f, 5.92f)
            reflectiveQuadTo(3.93f, 5.6f)
            horizontalLineTo(20.07f)
            quadToRelative(0.48f, 0f, 0.81f, 0.33f)
            reflectiveQuadTo(21.2f, 6.74f)
            reflectiveQuadTo(20.87f, 7.54f)
            reflectiveQuadTo(20.07f, 7.87f)
            horizontalLineTo(3.93f)
            close()
          }
        }
        .build().also { _sortW500 = it }
    }

private var _sortW300: ImageVector? = null
private var _sortW400: ImageVector? = null
private var _sortW500: ImageVector? = null

public val FujiIcons.SortByAlpha: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _sortByAlphaW300 ?: ImageVector.Builder(
          name = "sort_by_alpha",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.83f, 14.35f)
            lineToRelative(-0.71f, 1.9f)
            quadToRelative(-0.08f, 0.2f, -0.25f, 0.32f)
            reflectiveQuadTo(3.48f, 16.69f)
            quadToRelative(-0.37f, 0f, -0.58f, -0.3f)
            reflectiveQuadTo(2.83f, 15.75f)
            lineTo(6f, 7.75f)
            quadTo(6.08f, 7.55f, 6.25f, 7.43f)
            reflectiveQuadTo(6.64f, 7.31f)
            horizontalLineTo(7.15f)
            quadToRelative(0.22f, 0f, 0.39f, 0.12f)
            reflectiveQuadTo(7.81f, 7.75f)
            lineToRelative(3.13f, 8f)
            quadToRelative(0.12f, 0.35f, -0.08f, 0.64f)
            reflectiveQuadToRelative(-0.57f, 0.29f)
            quadToRelative(-0.22f, 0f, -0.4f, -0.13f)
            reflectiveQuadTo(9.63f, 16.22f)
            lineTo(8.95f, 14.35f)
            horizontalLineTo(4.83f)
            close()
            moveTo(5.27f, 13.13f)
            horizontalLineTo(8.45f)
            lineTo(6.94f, 8.94f)
            horizontalLineTo(6.81f)
            lineTo(5.27f, 13.13f)
            close()
            moveToRelative(10.27f, 2.3f)
            horizontalLineToRelative(4.48f)
            quadToRelative(0.26f, 0f, 0.44f, 0.18f)
            reflectiveQuadToRelative(0.18f, 0.44f)
            reflectiveQuadTo(20.46f, 16.5f)
            reflectiveQuadToRelative(-0.44f, 0.18f)
            horizontalLineTo(14.31f)
            quadToRelative(-0.17f, 0f, -0.28f, -0.11f)
            reflectiveQuadTo(13.91f, 16.3f)
            verticalLineTo(15.65f)
            quadToRelative(0f, -0.14f, 0.04f, -0.27f)
            reflectiveQuadToRelative(0.13f, -0.25f)
            lineTo(19f, 8.57f)
            horizontalLineTo(14.74f)
            quadToRelative(-0.27f, 0f, -0.45f, -0.18f)
            reflectiveQuadTo(14.11f, 7.94f)
            reflectiveQuadTo(14.29f, 7.5f)
            reflectiveQuadTo(14.74f, 7.31f)
            horizontalLineTo(20.2f)
            quadToRelative(0.17f, 0f, 0.28f, 0.11f)
            reflectiveQuadTo(20.6f, 7.7f)
            verticalLineTo(8.35f)
            quadToRelative(0f, 0.14f, -0.04f, 0.27f)
            reflectiveQuadTo(20.43f, 8.86f)
            lineToRelative(-4.89f, 6.57f)
            close()
            moveTo(9.83f, 4.96f)
            quadTo(9.67f, 4.96f, 9.62f, 4.84f)
            reflectiveQuadTo(9.68f, 4.6f)
            lineTo(11.7f, 2.58f)
            quadTo(11.83f, 2.45f, 12f, 2.45f)
            reflectiveQuadToRelative(0.3f, 0.13f)
            lineTo(14.32f, 4.6f)
            quadToRelative(0.11f, 0.11f, 0.06f, 0.24f)
            reflectiveQuadTo(14.17f, 4.96f)
            horizontalLineTo(9.83f)
            close()
            moveTo(11.7f, 21.42f)
            lineTo(9.68f, 19.4f)
            quadTo(9.56f, 19.29f, 9.62f, 19.16f)
            reflectiveQuadTo(9.83f, 19.04f)
            horizontalLineToRelative(4.35f)
            quadToRelative(0.15f, 0f, 0.21f, 0.12f)
            reflectiveQuadTo(14.32f, 19.4f)
            lineTo(12.3f, 21.42f)
            quadTo(12.17f, 21.55f, 12f, 21.55f)
            reflectiveQuadTo(11.7f, 21.42f)
            close()
          }
        }
        .build().also { _sortByAlphaW300 = it }
        IconWeight.W400 -> _sortByAlphaW400 ?: ImageVector.Builder(
          name = "sort_by_alpha",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.9f, 14.6f)
            lineTo(4.33f, 16.35f)
            quadToRelative(-0.1f, 0.27f, -0.35f, 0.46f)
            quadTo(3.73f, 17f, 3.43f, 17f)
            quadTo(2.93f, 17f, 2.61f, 16.59f)
            quadTo(2.3f, 16.18f, 2.5f, 15.68f)
            lineToRelative(3f, -8.03f)
            quadTo(5.63f, 7.35f, 5.88f, 7.18f)
            reflectiveQuadTo(6.45f, 7f)
            horizontalLineTo(7.2f)
            quadTo(7.53f, 7f, 7.78f, 7.18f)
            reflectiveQuadTo(8.15f, 7.65f)
            lineToRelative(3.03f, 8.08f)
            quadToRelative(0.18f, 0.47f, -0.11f, 0.88f)
            quadTo(10.78f, 17f, 10.28f, 17f)
            quadTo(9.98f, 17f, 9.73f, 16.81f)
            reflectiveQuadTo(9.38f, 16.35f)
            lineTo(8.75f, 14.6f)
            horizontalLineTo(4.9f)
            close()
            moveTo(5.5f, 12.9f)
            horizontalLineTo(8.1f)
            lineTo(6.9f, 9.15f)
            horizontalLineTo(6.75f)
            lineTo(5.5f, 12.9f)
            close()
            moveToRelative(10.45f, 2.3f)
            horizontalLineTo(20.1f)
            quadToRelative(0.38f, 0f, 0.64f, 0.26f)
            reflectiveQuadTo(21f, 16.1f)
            reflectiveQuadToRelative(-0.26f, 0.64f)
            reflectiveQuadTo(20.1f, 17f)
            horizontalLineTo(14.3f)
            quadToRelative(-0.25f, 0f, -0.43f, -0.18f)
            reflectiveQuadTo(13.7f, 16.4f)
            verticalLineTo(15.45f)
            quadToRelative(0f, -0.17f, 0.05f, -0.34f)
            reflectiveQuadToRelative(0.18f, -0.29f)
            lineTo(18.75f, 8.8f)
            horizontalLineTo(14.8f)
            quadToRelative(-0.38f, 0f, -0.64f, -0.26f)
            quadTo(13.9f, 8.27f, 13.9f, 7.9f)
            reflectiveQuadTo(14.16f, 7.26f)
            reflectiveQuadTo(14.8f, 7f)
            horizontalLineToRelative(5.55f)
            quadToRelative(0.25f, 0f, 0.43f, 0.18f)
            reflectiveQuadTo(20.95f, 7.6f)
            verticalLineTo(8.55f)
            quadToRelative(0f, 0.18f, -0.05f, 0.34f)
            reflectiveQuadTo(20.73f, 9.17f)
            lineTo(15.95f, 15.2f)
            close()
            moveTo(9.6f, 5f)
            quadTo(9.43f, 5f, 9.36f, 4.85f)
            reflectiveQuadTo(9.43f, 4.57f)
            lineTo(11.65f, 2.35f)
            quadTo(11.8f, 2.2f, 12f, 2.2f)
            reflectiveQuadToRelative(0.35f, 0.15f)
            lineToRelative(2.22f, 2.23f)
            quadTo(14.7f, 4.7f, 14.64f, 4.85f)
            reflectiveQuadTo(14.4f, 5f)
            horizontalLineTo(9.6f)
            close()
            moveToRelative(2.05f, 16.65f)
            lineTo(9.43f, 19.43f)
            quadTo(9.3f, 19.3f, 9.36f, 19.15f)
            reflectiveQuadTo(9.6f, 19f)
            horizontalLineToRelative(4.8f)
            quadToRelative(0.18f, 0f, 0.24f, 0.15f)
            reflectiveQuadToRelative(-0.06f, 0.28f)
            lineToRelative(-2.22f, 2.22f)
            quadTo(12.2f, 21.8f, 12f, 21.8f)
            reflectiveQuadTo(11.65f, 21.65f)
            close()
          }
        }
        .build().also { _sortByAlphaW400 = it }
        IconWeight.W500 -> _sortByAlphaW500 ?: ImageVector.Builder(
          name = "sort_by_alpha",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4.92f, 14.65f)
            lineTo(4.35f, 16.37f)
            quadTo(4.24f, 16.66f, 3.98f, 16.85f)
            reflectiveQuadTo(3.41f, 17.05f)
            quadToRelative(-0.53f, 0f, -0.85f, -0.44f)
            reflectiveQuadTo(2.44f, 15.66f)
            lineToRelative(3f, -8.03f)
            quadTo(5.57f, 7.32f, 5.83f, 7.14f)
            reflectiveQuadTo(6.43f, 6.95f)
            horizontalLineTo(7.22f)
            quadToRelative(0.34f, 0f, 0.6f, 0.18f)
            reflectiveQuadToRelative(0.39f, 0.5f)
            lineToRelative(3.01f, 8.06f)
            quadToRelative(0.19f, 0.5f, -0.12f, 0.93f)
            reflectiveQuadToRelative(-0.84f, 0.43f)
            quadToRelative(-0.32f, 0f, -0.59f, -0.2f)
            reflectiveQuadTo(9.3f, 16.36f)
            lineTo(8.7f, 14.65f)
            horizontalLineTo(4.92f)
            close()
            moveToRelative(0.63f, -1.8f)
            horizontalLineTo(8.02f)
            lineTo(6.88f, 9.31f)
            horizontalLineTo(6.73f)
            lineTo(5.55f, 12.85f)
            close()
            moveToRelative(10.55f, 2.3f)
            horizontalLineToRelative(4.02f)
            quadToRelative(0.39f, 0f, 0.67f, 0.28f)
            reflectiveQuadToRelative(0.28f, 0.67f)
            reflectiveQuadToRelative(-0.28f, 0.67f)
            reflectiveQuadToRelative(-0.67f, 0.28f)
            horizontalLineTo(14.29f)
            quadToRelative(-0.26f, 0f, -0.44f, -0.18f)
            reflectiveQuadTo(13.67f, 16.43f)
            verticalLineTo(15.48f)
            quadToRelative(0f, -0.2f, 0.06f, -0.38f)
            reflectiveQuadToRelative(0.19f, -0.33f)
            lineTo(18.64f, 8.85f)
            horizontalLineTo(14.82f)
            quadToRelative(-0.39f, 0f, -0.67f, -0.28f)
            reflectiveQuadTo(13.87f, 7.9f)
            reflectiveQuadTo(14.15f, 7.23f)
            reflectiveQuadTo(14.82f, 6.95f)
            horizontalLineTo(20.4f)
            quadToRelative(0.26f, 0f, 0.43f, 0.18f)
            reflectiveQuadToRelative(0.18f, 0.43f)
            verticalLineTo(8.53f)
            quadToRelative(0f, 0.2f, -0.06f, 0.38f)
            reflectiveQuadTo(20.77f, 9.23f)
            lineTo(16.1f, 15.15f)
            close()
            moveTo(9.58f, 5.05f)
            quadToRelative(-0.2f, 0f, -0.27f, -0.17f)
            reflectiveQuadTo(9.38f, 4.56f)
            lineTo(11.6f, 2.34f)
            quadTo(11.77f, 2.17f, 12f, 2.17f)
            reflectiveQuadToRelative(0.4f, 0.17f)
            lineToRelative(2.23f, 2.22f)
            quadToRelative(0.14f, 0.14f, 0.07f, 0.31f)
            reflectiveQuadTo(14.42f, 5.05f)
            horizontalLineTo(9.58f)
            close()
            moveTo(11.6f, 21.66f)
            lineTo(9.38f, 19.44f)
            quadTo(9.23f, 19.29f, 9.31f, 19.12f)
            reflectiveQuadTo(9.58f, 18.95f)
            horizontalLineToRelative(4.85f)
            quadToRelative(0.2f, 0f, 0.27f, 0.17f)
            reflectiveQuadToRelative(-0.07f, 0.31f)
            lineTo(12.4f, 21.66f)
            quadTo(12.23f, 21.83f, 12f, 21.83f)
            reflectiveQuadTo(11.6f, 21.66f)
            close()
          }
        }
        .build().also { _sortByAlphaW500 = it }
    }

private var _sortByAlphaW300: ImageVector? = null
private var _sortByAlphaW400: ImageVector? = null
private var _sortByAlphaW500: ImageVector? = null

public val FujiIcons.Star: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _starW300 ?: ImageVector.Builder(
          name = "star",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.77f, 16.93f)
            lineTo(12f, 14.99f)
            lineToRelative(3.23f, 1.97f)
            lineTo(14.38f, 13.27f)
            lineToRelative(2.86f, -2.46f)
            lineTo(13.48f, 10.47f)
            lineTo(12f, 6.99f)
            lineToRelative(-1.48f, 3.47f)
            lineTo(6.77f, 10.78f)
            lineToRelative(2.86f, 2.47f)
            lineTo(8.77f, 16.93f)
            close()
            moveTo(12f, 16.63f)
            lineTo(8.08f, 18.99f)
            quadToRelative(-0.2f, 0.12f, -0.41f, 0.1f)
            reflectiveQuadTo(7.3f, 18.95f)
            reflectiveQuadTo(7.05f, 18.64f)
            reflectiveQuadTo(7.03f, 18.22f)
            lineTo(8.07f, 13.77f)
            lineTo(4.61f, 10.78f)
            quadTo(4.43f, 10.62f, 4.38f, 10.42f)
            reflectiveQuadTo(4.4f, 10.02f)
            reflectiveQuadTo(4.62f, 9.7f)
            reflectiveQuadTo(5.02f, 9.54f)
            lineTo(9.57f, 9.14f)
            lineToRelative(1.78f, -4.2f)
            quadTo(11.44f, 4.73f, 11.62f, 4.62f)
            reflectiveQuadTo(12f, 4.52f)
            reflectiveQuadToRelative(0.38f, 0.1f)
            reflectiveQuadToRelative(0.27f, 0.32f)
            lineToRelative(1.78f, 4.2f)
            lineToRelative(4.55f, 0.4f)
            quadToRelative(0.24f, 0.03f, 0.39f, 0.16f)
            reflectiveQuadToRelative(0.22f, 0.32f)
            reflectiveQuadToRelative(0.02f, 0.39f)
            reflectiveQuadToRelative(-0.23f, 0.36f)
            lineToRelative(-3.45f, 2.99f)
            lineToRelative(1.05f, 4.45f)
            quadToRelative(0.06f, 0.23f, -0.03f, 0.42f)
            reflectiveQuadTo(16.7f, 18.95f)
            reflectiveQuadToRelative(-0.37f, 0.14f)
            reflectiveQuadToRelative(-0.41f, -0.1f)
            lineTo(12f, 16.63f)
            close()
            moveToRelative(0f, -4.4f)
            close()
          }
        }
        .build().also { _starW300 = it }
        IconWeight.W400 -> _starW400 ?: ImageVector.Builder(
          name = "star",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.85f, 16.83f)
            lineTo(12f, 14.93f)
            lineToRelative(3.15f, 1.93f)
            lineToRelative(-0.82f, -3.6f)
            lineToRelative(2.78f, -2.4f)
            lineTo(13.45f, 10.52f)
            lineTo(12f, 7.13f)
            lineTo(10.55f, 10.5f)
            lineTo(6.9f, 10.83f)
            lineToRelative(2.78f, 2.43f)
            lineTo(8.85f, 16.83f)
            close()
            moveTo(12f, 17.27f)
            lineToRelative(-4.15f, 2.5f)
            quadTo(7.58f, 19.95f, 7.28f, 19.93f)
            reflectiveQuadTo(6.75f, 19.73f)
            reflectiveQuadTo(6.4f, 19.29f)
            quadTo(6.28f, 19.02f, 6.35f, 18.7f)
            lineToRelative(1.1f, -4.72f)
            lineTo(3.78f, 10.8f)
            quadTo(3.53f, 10.58f, 3.46f, 10.29f)
            reflectiveQuadTo(3.5f, 9.73f)
            reflectiveQuadTo(3.8f, 9.27f)
            reflectiveQuadTo(4.35f, 9.05f)
            lineTo(9.2f, 8.63f)
            lineTo(11.08f, 4.17f)
            quadTo(11.2f, 3.88f, 11.46f, 3.72f)
            reflectiveQuadTo(12f, 3.57f)
            quadToRelative(0.28f, 0f, 0.54f, 0.15f)
            quadToRelative(0.26f, 0.15f, 0.39f, 0.45f)
            lineTo(14.8f, 8.63f)
            lineToRelative(4.85f, 0.42f)
            quadTo(20f, 9.1f, 20.2f, 9.27f)
            reflectiveQuadToRelative(0.3f, 0.45f)
            reflectiveQuadToRelative(0.04f, 0.56f)
            reflectiveQuadTo(20.23f, 10.8f)
            lineToRelative(-3.68f, 3.18f)
            lineToRelative(1.1f, 4.72f)
            quadToRelative(0.07f, 0.32f, -0.05f, 0.59f)
            reflectiveQuadToRelative(-0.35f, 0.44f)
            quadToRelative(-0.22f, 0.17f, -0.52f, 0.2f)
            reflectiveQuadTo(16.15f, 19.77f)
            lineTo(12f, 17.27f)
            close()
            moveToRelative(0f, -5.02f)
            close()
          }
        }
        .build().also { _starW400 = it }
        IconWeight.W500 -> _starW500 ?: ImageVector.Builder(
          name = "star",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.96f, 16.66f)
            lineTo(12f, 14.84f)
            lineToRelative(3.04f, 1.85f)
            lineToRelative(-0.8f, -3.46f)
            lineToRelative(2.68f, -2.32f)
            lineTo(13.4f, 10.6f)
            lineTo(12f, 7.32f)
            lineToRelative(-1.4f, 3.26f)
            lineTo(7.09f, 10.88f)
            lineToRelative(2.68f, 2.34f)
            lineToRelative(-0.8f, 3.44f)
            close()
            moveTo(12f, 17.5f)
            lineTo(7.83f, 20.01f)
            quadToRelative(-0.32f, 0.2f, -0.66f, 0.17f)
            reflectiveQuadTo(6.58f, 19.96f)
            reflectiveQuadTo(6.18f, 19.46f)
            reflectiveQuadTo(6.13f, 18.79f)
            lineTo(7.23f, 14.05f)
            lineTo(3.54f, 10.85f)
            quadTo(3.26f, 10.6f, 3.18f, 10.27f)
            reflectiveQuadTo(3.22f, 9.64f)
            reflectiveQuadTo(3.56f, 9.12f)
            reflectiveQuadTo(4.19f, 8.86f)
            lineTo(9.06f, 8.44f)
            lineTo(10.95f, 3.97f)
            quadTo(11.09f, 3.62f, 11.39f, 3.45f)
            reflectiveQuadTo(12f, 3.28f)
            reflectiveQuadToRelative(0.61f, 0.17f)
            reflectiveQuadToRelative(0.44f, 0.52f)
            lineToRelative(1.89f, 4.47f)
            lineToRelative(4.87f, 0.43f)
            quadToRelative(0.39f, 0.05f, 0.63f, 0.25f)
            reflectiveQuadToRelative(0.34f, 0.52f)
            reflectiveQuadToRelative(0.03f, 0.64f)
            reflectiveQuadToRelative(-0.36f, 0.58f)
            lineToRelative(-3.69f, 3.19f)
            lineToRelative(1.11f, 4.74f)
            quadToRelative(0.09f, 0.37f, -0.05f, 0.67f)
            reflectiveQuadToRelative(-0.39f, 0.5f)
            reflectiveQuadToRelative(-0.6f, 0.23f)
            reflectiveQuadTo(16.17f, 20.01f)
            lineTo(12f, 17.5f)
            close()
            moveToRelative(0f, -5.23f)
            close()
          }
        }
        .build().also { _starW500 = it }
    }

private var _starW300: ImageVector? = null
private var _starW400: ImageVector? = null
private var _starW500: ImageVector? = null

public val FujiIcons.StarRate: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _starRateW300 ?: ImageVector.Builder(
          name = "star_rate",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 15.54f)
            lineTo(8.73f, 18.03f)
            quadTo(8.51f, 18.18f, 8.29f, 18.17f)
            reflectiveQuadTo(7.89f, 18.03f)
            reflectiveQuadTo(7.63f, 17.7f)
            reflectiveQuadTo(7.62f, 17.25f)
            lineTo(8.87f, 13.17f)
            lineTo(5.71f, 10.88f)
            quadTo(5.49f, 10.73f, 5.43f, 10.51f)
            reflectiveQuadTo(5.45f, 10.09f)
            reflectiveQuadTo(5.69f, 9.74f)
            reflectiveQuadTo(6.12f, 9.6f)
            horizontalLineToRelative(3.93f)
            lineTo(11.33f, 5.38f)
            quadTo(11.42f, 5.13f, 11.6f, 5f)
            reflectiveQuadTo(12f, 4.88f)
            reflectiveQuadTo(12.4f, 5f)
            reflectiveQuadToRelative(0.27f, 0.38f)
            lineTo(13.95f, 9.6f)
            horizontalLineToRelative(3.93f)
            quadToRelative(0.25f, 0f, 0.42f, 0.14f)
            reflectiveQuadToRelative(0.24f, 0.35f)
            reflectiveQuadToRelative(0.01f, 0.42f)
            reflectiveQuadToRelative(-0.27f, 0.37f)
            lineToRelative(-3.16f, 2.28f)
            lineToRelative(1.25f, 4.08f)
            quadToRelative(0.08f, 0.25f, -0.01f, 0.46f)
            reflectiveQuadToRelative(-0.27f, 0.33f)
            reflectiveQuadToRelative(-0.4f, 0.13f)
            reflectiveQuadTo(15.27f, 18.03f)
            lineTo(12f, 15.54f)
            close()
          }
        }
        .build().also { _starRateW300 = it }
        IconWeight.W400 -> _starRateW400 ?: ImageVector.Builder(
          name = "star_rate",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 16.3f)
            lineTo(8.3f, 19.13f)
            quadTo(8.03f, 19.35f, 7.7f, 19.34f)
            quadTo(7.38f, 19.33f, 7.13f, 19.15f)
            quadTo(6.88f, 18.98f, 6.74f, 18.68f)
            reflectiveQuadTo(6.73f, 18.02f)
            lineTo(8.15f, 13.4f)
            lineTo(4.53f, 10.83f)
            quadTo(4.23f, 10.63f, 4.15f, 10.3f)
            quadTo(4.08f, 9.98f, 4.18f, 9.7f)
            quadTo(4.28f, 9.42f, 4.53f, 9.21f)
            reflectiveQuadTo(5.13f, 9f)
            horizontalLineTo(9.6f)
            lineTo(11.05f, 4.2f)
            quadTo(11.18f, 3.85f, 11.44f, 3.66f)
            reflectiveQuadTo(12f, 3.47f)
            reflectiveQuadToRelative(0.56f, 0.19f)
            reflectiveQuadTo(12.95f, 4.2f)
            lineTo(14.4f, 9f)
            horizontalLineToRelative(4.47f)
            quadToRelative(0.35f, 0f, 0.6f, 0.21f)
            reflectiveQuadTo(19.83f, 9.7f)
            reflectiveQuadToRelative(0.02f, 0.6f)
            quadToRelative(-0.07f, 0.33f, -0.38f, 0.53f)
            lineTo(15.85f, 13.4f)
            lineToRelative(1.43f, 4.63f)
            quadToRelative(0.13f, 0.35f, -0.01f, 0.65f)
            quadToRelative(-0.14f, 0.3f, -0.39f, 0.47f)
            quadToRelative(-0.25f, 0.18f, -0.57f, 0.19f)
            reflectiveQuadTo(15.7f, 19.13f)
            lineTo(12f, 16.3f)
            close()
          }
        }
        .build().also { _starRateW400 = it }
        IconWeight.W500 -> _starRateW500 ?: ImageVector.Builder(
          name = "star_rate",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12.01f, 16.45f)
            lineTo(8.44f, 19.17f)
            quadTo(8.12f, 19.43f, 7.75f, 19.41f)
            reflectiveQuadTo(7.1f, 19.2f)
            reflectiveQuadTo(6.66f, 18.66f)
            reflectiveQuadTo(6.65f, 17.92f)
            lineTo(8.02f, 13.45f)
            lineTo(4.53f, 10.97f)
            quadTo(4.19f, 10.73f, 4.1f, 10.37f)
            reflectiveQuadTo(4.13f, 9.69f)
            reflectiveQuadTo(4.52f, 9.14f)
            reflectiveQuadTo(5.21f, 8.9f)
            horizontalLineToRelative(4.3f)
            lineTo(10.92f, 4.25f)
            quadToRelative(0.14f, -0.4f, 0.44f, -0.61f)
            reflectiveQuadTo(12f, 3.43f)
            reflectiveQuadToRelative(0.65f, 0.21f)
            reflectiveQuadToRelative(0.44f, 0.61f)
            lineTo(14.49f, 8.9f)
            horizontalLineToRelative(4.3f)
            quadToRelative(0.4f, 0f, 0.69f, 0.24f)
            reflectiveQuadToRelative(0.39f, 0.56f)
            reflectiveQuadToRelative(0.02f, 0.68f)
            reflectiveQuadToRelative(-0.43f, 0.6f)
            lineToRelative(-3.49f, 2.49f)
            lineToRelative(1.38f, 4.46f)
            quadToRelative(0.14f, 0.4f, -0.01f, 0.74f)
            reflectiveQuadTo(16.9f, 19.19f)
            reflectiveQuadToRelative(-0.65f, 0.21f)
            reflectiveQuadTo(15.57f, 19.17f)
            lineTo(12.01f, 16.45f)
            close()
          }
        }
        .build().also { _starRateW500 = it }
    }

private var _starRateW300: ImageVector? = null
private var _starRateW400: ImageVector? = null
private var _starRateW500: ImageVector? = null

public val FujiIcons.TextCompare: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _textCompareW300 ?: ImageVector.Builder(
          name = "text_compare",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10.6f, 21.71f)
            verticalLineTo(20.32f)
            horizontalLineTo(5.39f)
            quadToRelative(-0.71f, 0f, -1.21f, -0.5f)
            reflectiveQuadTo(3.68f, 18.61f)
            verticalLineTo(5.39f)
            quadToRelative(0f, -0.71f, 0.5f, -1.21f)
            reflectiveQuadTo(5.39f, 3.68f)
            horizontalLineTo(10.6f)
            verticalLineTo(2.28f)
            quadTo(10.6f, 2f, 10.81f, 1.79f)
            reflectiveQuadTo(11.3f, 1.59f)
            reflectiveQuadToRelative(0.49f, 0.21f)
            reflectiveQuadTo(12f, 2.28f)
            verticalLineTo(21.71f)
            quadToRelative(0f, 0.29f, -0.21f, 0.5f)
            reflectiveQuadTo(11.3f, 22.41f)
            reflectiveQuadTo(10.81f, 22.21f)
            reflectiveQuadTo(10.6f, 21.71f)
            close()
            moveTo(5.39f, 18.92f)
            horizontalLineTo(10.6f)
            verticalLineTo(16.36f)
            horizontalLineTo(8.05f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(7.35f, 15.66f)
            reflectiveQuadTo(7.56f, 15.16f)
            reflectiveQuadToRelative(0.5f, -0.21f)
            horizontalLineTo(10.6f)
            verticalLineTo(12.7f)
            horizontalLineTo(8.05f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.2f)
            reflectiveQuadTo(7.35f, 12f)
            reflectiveQuadTo(7.56f, 11.5f)
            reflectiveQuadTo(8.05f, 11.3f)
            horizontalLineTo(10.6f)
            verticalLineTo(9.04f)
            horizontalLineTo(8.05f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(7.35f, 8.34f)
            reflectiveQuadTo(7.56f, 7.85f)
            reflectiveQuadTo(8.05f, 7.64f)
            horizontalLineTo(10.6f)
            verticalLineTo(5.08f)
            horizontalLineTo(5.39f)
            quadToRelative(-0.12f, 0f, -0.21f, 0.1f)
            reflectiveQuadTo(5.08f, 5.39f)
            verticalLineTo(18.61f)
            quadToRelative(0f, 0.12f, 0.1f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.1f)
            close()
            moveToRelative(9.31f, 1.4f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(14f, 19.62f)
            reflectiveQuadTo(14.2f, 19.12f)
            reflectiveQuadToRelative(0.5f, -0.21f)
            horizontalLineToRelative(4.01f)
            quadToRelative(0.12f, 0f, 0.21f, -0.1f)
            reflectiveQuadToRelative(0.1f, -0.21f)
            verticalLineTo(5.39f)
            quadToRelative(0f, -0.12f, -0.1f, -0.21f)
            reflectiveQuadTo(18.71f, 5.08f)
            horizontalLineTo(14.7f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(14f, 4.38f)
            reflectiveQuadTo(14.2f, 3.89f)
            reflectiveQuadTo(14.7f, 3.68f)
            horizontalLineToRelative(4.01f)
            quadToRelative(0.71f, 0f, 1.21f, 0.5f)
            reflectiveQuadToRelative(0.5f, 1.21f)
            verticalLineTo(18.61f)
            quadToRelative(0f, 0.71f, -0.5f, 1.21f)
            reflectiveQuadToRelative(-1.21f, 0.5f)
            horizontalLineTo(14.7f)
            close()
            moveToRelative(0f, -7.62f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.2f)
            reflectiveQuadTo(14f, 12f)
            reflectiveQuadTo(14.2f, 11.5f)
            reflectiveQuadTo(14.7f, 11.3f)
            horizontalLineToRelative(1.85f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadTo(17.25f, 12f)
            reflectiveQuadToRelative(-0.21f, 0.49f)
            reflectiveQuadToRelative(-0.49f, 0.2f)
            horizontalLineTo(14.7f)
            close()
            moveToRelative(0f, -3.66f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(14f, 8.34f)
            reflectiveQuadTo(14.2f, 7.85f)
            reflectiveQuadTo(14.7f, 7.64f)
            horizontalLineToRelative(1.85f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            reflectiveQuadTo(17.04f, 8.84f)
            reflectiveQuadTo(16.55f, 9.04f)
            horizontalLineTo(14.7f)
            close()
            moveTo(10.6f, 12f)
            close()
          }
        }
        .build().also { _textCompareW300 = it }
        IconWeight.W400 -> _textCompareW400 ?: ImageVector.Builder(
          name = "text_compare",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(10f, 22f)
            verticalLineTo(21f)
            horizontalLineTo(5f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineToRelative(5f)
            verticalLineTo(2f)
            quadTo(10f, 1.57f, 10.29f, 1.29f)
            reflectiveQuadTo(11f, 1f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(12f, 2f)
            verticalLineTo(22f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(11f, 23f)
            reflectiveQuadTo(10.29f, 22.71f)
            quadTo(10f, 22.43f, 10f, 22f)
            close()
            moveTo(5f, 19f)
            horizontalLineToRelative(5f)
            verticalLineTo(17f)
            horizontalLineTo(8f)
            quadTo(7.58f, 17f, 7.29f, 16.71f)
            quadTo(7f, 16.43f, 7f, 16f)
            reflectiveQuadTo(7.29f, 15.29f)
            reflectiveQuadTo(8f, 15f)
            horizontalLineToRelative(2f)
            verticalLineTo(13f)
            horizontalLineTo(8f)
            quadTo(7.58f, 13f, 7.29f, 12.71f)
            quadTo(7f, 12.43f, 7f, 12f)
            reflectiveQuadTo(7.29f, 11.29f)
            reflectiveQuadTo(8f, 11f)
            horizontalLineToRelative(2f)
            verticalLineTo(9f)
            horizontalLineTo(8f)
            quadTo(7.58f, 9f, 7.29f, 8.71f)
            reflectiveQuadTo(7f, 8f)
            quadTo(7f, 7.57f, 7.29f, 7.29f)
            reflectiveQuadTo(8f, 7f)
            horizontalLineToRelative(2f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            close()
            moveToRelative(10f, 2f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(14f, 20.43f, 14f, 20f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(15f, 19f)
            horizontalLineToRelative(4f)
            verticalLineTo(5f)
            horizontalLineTo(15f)
            quadTo(14.58f, 5f, 14.29f, 4.71f)
            reflectiveQuadTo(14f, 4f)
            quadTo(14f, 3.57f, 14.29f, 3.29f)
            reflectiveQuadTo(15f, 3f)
            horizontalLineToRelative(4f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            reflectiveQuadTo(21f, 5f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(15f)
            close()
            moveToRelative(0f, -8f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(14f, 12.43f, 14f, 12f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(15f, 11f)
            horizontalLineToRelative(1f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(16f, 13f)
            horizontalLineTo(15f)
            close()
            moveTo(15f, 9f)
            quadTo(14.58f, 9f, 14.29f, 8.71f)
            reflectiveQuadTo(14f, 8f)
            quadTo(14f, 7.57f, 14.29f, 7.29f)
            reflectiveQuadTo(15f, 7f)
            horizontalLineToRelative(1f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(16f, 9f)
            horizontalLineTo(15f)
            close()
            moveToRelative(-5f, 3f)
            close()
          }
        }
        .build().also { _textCompareW400 = it }
        IconWeight.W500 -> _textCompareW500 ?: ImageVector.Builder(
          name = "text_compare",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.91f, 22.14f)
            verticalLineTo(21.2f)
            horizontalLineTo(5.07f)
            quadToRelative(-0.94f, 0f, -1.61f, -0.67f)
            reflectiveQuadTo(2.8f, 18.93f)
            verticalLineTo(5.07f)
            quadTo(2.8f, 4.13f, 3.46f, 3.46f)
            reflectiveQuadTo(5.07f, 2.8f)
            horizontalLineTo(9.91f)
            verticalLineTo(1.86f)
            quadToRelative(0f, -0.44f, 0.3f, -0.74f)
            reflectiveQuadToRelative(0.75f, -0.3f)
            reflectiveQuadToRelative(0.74f, 0.3f)
            reflectiveQuadTo(12f, 1.86f)
            verticalLineTo(22.14f)
            quadToRelative(0f, 0.44f, -0.3f, 0.74f)
            reflectiveQuadToRelative(-0.74f, 0.3f)
            reflectiveQuadToRelative(-0.75f, -0.3f)
            reflectiveQuadTo(9.91f, 22.14f)
            close()
            moveTo(5.07f, 18.93f)
            horizontalLineTo(9.91f)
            verticalLineTo(16.95f)
            horizontalLineTo(7.95f)
            quadToRelative(-0.43f, 0f, -0.72f, -0.3f)
            reflectiveQuadTo(6.93f, 15.93f)
            reflectiveQuadTo(7.23f, 15.2f)
            reflectiveQuadTo(7.96f, 14.91f)
            horizontalLineTo(9.91f)
            verticalLineTo(13.05f)
            horizontalLineTo(7.96f)
            quadToRelative(-0.43f, 0f, -0.73f, -0.3f)
            reflectiveQuadTo(6.93f, 12.02f)
            reflectiveQuadTo(7.23f, 11.3f)
            reflectiveQuadTo(7.96f, 11f)
            horizontalLineTo(9.91f)
            verticalLineTo(9.14f)
            horizontalLineTo(7.96f)
            quadToRelative(-0.43f, 0f, -0.73f, -0.3f)
            reflectiveQuadTo(6.93f, 8.11f)
            reflectiveQuadTo(7.23f, 7.39f)
            reflectiveQuadTo(7.96f, 7.09f)
            horizontalLineTo(9.91f)
            verticalLineTo(5.07f)
            horizontalLineTo(5.07f)
            verticalLineTo(18.93f)
            close()
            moveTo(15.23f, 21.2f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(14.09f, 20.07f)
            reflectiveQuadToRelative(0.33f, -0.81f)
            reflectiveQuadToRelative(0.81f, -0.33f)
            horizontalLineToRelative(3.7f)
            verticalLineTo(5.07f)
            horizontalLineToRelative(-3.7f)
            quadToRelative(-0.48f, 0f, -0.81f, -0.33f)
            reflectiveQuadTo(14.09f, 3.93f)
            reflectiveQuadTo(14.42f, 3.13f)
            reflectiveQuadTo(15.23f, 2.8f)
            horizontalLineToRelative(3.7f)
            quadToRelative(0.94f, 0f, 1.61f, 0.67f)
            reflectiveQuadTo(21.2f, 5.07f)
            verticalLineTo(18.93f)
            quadToRelative(0f, 0.94f, -0.67f, 1.61f)
            reflectiveQuadTo(18.93f, 21.2f)
            horizontalLineToRelative(-3.7f)
            close()
            moveTo(15.11f, 13.05f)
            quadToRelative(-0.43f, 0f, -0.73f, -0.3f)
            reflectiveQuadToRelative(-0.3f, -0.73f)
            reflectiveQuadToRelative(0.3f, -0.73f)
            reflectiveQuadTo(15.11f, 11f)
            horizontalLineToRelative(0.93f)
            quadToRelative(0.43f, 0f, 0.73f, 0.3f)
            reflectiveQuadToRelative(0.3f, 0.73f)
            reflectiveQuadToRelative(-0.3f, 0.73f)
            reflectiveQuadToRelative(-0.73f, 0.3f)
            horizontalLineTo(15.11f)
            close()
            moveToRelative(0f, -3.91f)
            quadToRelative(-0.43f, 0f, -0.73f, -0.3f)
            reflectiveQuadTo(14.09f, 8.11f)
            reflectiveQuadToRelative(0.3f, -0.73f)
            reflectiveQuadToRelative(0.73f, -0.3f)
            horizontalLineToRelative(0.93f)
            quadToRelative(0.43f, 0f, 0.73f, 0.3f)
            reflectiveQuadToRelative(0.3f, 0.73f)
            reflectiveQuadToRelative(-0.3f, 0.73f)
            reflectiveQuadToRelative(-0.73f, 0.3f)
            horizontalLineTo(15.11f)
            close()
            moveTo(9.91f, 12f)
            close()
          }
        }
        .build().also { _textCompareW500 = it }
    }

private var _textCompareW300: ImageVector? = null
private var _textCompareW400: ImageVector? = null
private var _textCompareW500: ImageVector? = null

public val FujiIcons.Tonality: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _tonalityW300 ?: ImageVector.Builder(
          name = "tonality",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.38f, 20.57f)
            quadTo(6.68f, 19.83f, 5.42f, 18.58f)
            reflectiveQuadTo(3.44f, 15.63f)
            reflectiveQuadTo(2.7f, 12f)
            reflectiveQuadTo(3.43f, 8.38f)
            reflectiveQuadTo(5.42f, 5.42f)
            reflectiveQuadTo(8.37f, 3.44f)
            reflectiveQuadTo(12f, 2.7f)
            reflectiveQuadToRelative(3.63f, 0.73f)
            reflectiveQuadToRelative(2.95f, 1.99f)
            reflectiveQuadToRelative(1.99f, 2.95f)
            reflectiveQuadTo(21.3f, 12f)
            reflectiveQuadToRelative(-0.73f, 3.63f)
            reflectiveQuadToRelative(-1.99f, 2.95f)
            reflectiveQuadToRelative(-2.95f, 1.99f)
            reflectiveQuadTo(12f, 21.3f)
            reflectiveQuadTo(8.38f, 20.57f)
            close()
            moveToRelative(2.93f, -0.7f)
            verticalLineTo(4.13f)
            quadTo(8.24f, 4.53f, 6.17f, 6.74f)
            reflectiveQuadTo(4.1f, 12f)
            reflectiveQuadToRelative(2.07f, 5.26f)
            reflectiveQuadToRelative(5.13f, 2.61f)
            close()
            moveToRelative(1.4f, 0f)
            quadToRelative(0.96f, -0.08f, 1.87f, -0.37f)
            reflectiveQuadToRelative(1.68f, -0.88f)
            horizontalLineTo(12.7f)
            verticalLineToRelative(1.25f)
            close()
            moveToRelative(0f, -2.65f)
            horizontalLineToRelative(5.2f)
            quadToRelative(0.35f, -0.34f, 0.61f, -0.73f)
            reflectiveQuadToRelative(0.46f, -0.83f)
            horizontalLineTo(12.7f)
            verticalLineToRelative(1.56f)
            close()
            moveToRelative(0f, -2.96f)
            horizontalLineToRelative(6.87f)
            quadToRelative(0.13f, -0.38f, 0.2f, -0.77f)
            reflectiveQuadToRelative(0.1f, -0.79f)
            horizontalLineTo(12.7f)
            verticalLineToRelative(1.56f)
            close()
            moveToRelative(0f, -2.96f)
            horizontalLineToRelative(7.17f)
            quadToRelative(-0.03f, -0.4f, -0.1f, -0.78f)
            reflectiveQuadTo(19.57f, 9.74f)
            horizontalLineTo(12.7f)
            verticalLineTo(11.3f)
            close()
            moveToRelative(0f, -2.96f)
            horizontalLineToRelative(6.27f)
            quadTo(18.78f, 7.9f, 18.51f, 7.51f)
            reflectiveQuadTo(17.9f, 6.78f)
            horizontalLineTo(12.7f)
            verticalLineTo(8.34f)
            close()
            moveToRelative(0f, -2.96f)
            horizontalLineToRelative(3.55f)
            quadTo(15.49f, 4.8f, 14.58f, 4.5f)
            reflectiveQuadTo(12.7f, 4.13f)
            verticalLineTo(5.38f)
            close()
            moveTo(11.3f, 12f)
            close()
          }
        }
        .build().also { _tonalityW300 = it }
        IconWeight.W400 -> _tonalityW400 ?: ImageVector.Builder(
          name = "tonality",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            close()
            moveTo(11f, 19.95f)
            verticalLineTo(4.05f)
            quadTo(7.98f, 4.42f, 5.99f, 6.7f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(1.99f, 5.3f)
            reflectiveQuadTo(11f, 19.95f)
            close()
            moveToRelative(2f, 0f)
            quadToRelative(0.75f, -0.13f, 1.48f, -0.34f)
            reflectiveQuadTo(15.85f, 19f)
            horizontalLineTo(13f)
            verticalLineToRelative(0.95f)
            close()
            moveTo(13f, 17f)
            horizontalLineToRelative(5.25f)
            quadToRelative(0.2f, -0.23f, 0.35f, -0.48f)
            reflectiveQuadTo(18.9f, 16f)
            horizontalLineTo(13f)
            verticalLineToRelative(1f)
            close()
            moveToRelative(0f, -3f)
            horizontalLineToRelative(6.75f)
            quadToRelative(0.05f, -0.25f, 0.1f, -0.5f)
            reflectiveQuadTo(19.95f, 13f)
            horizontalLineTo(13f)
            verticalLineToRelative(1f)
            close()
            moveToRelative(0f, -3f)
            horizontalLineToRelative(6.95f)
            quadTo(19.9f, 10.75f, 19.85f, 10.5f)
            reflectiveQuadTo(19.75f, 10f)
            horizontalLineTo(13f)
            verticalLineToRelative(1f)
            close()
            moveTo(13f, 8f)
            horizontalLineToRelative(5.9f)
            quadTo(18.75f, 7.72f, 18.6f, 7.47f)
            reflectiveQuadTo(18.25f, 7f)
            horizontalLineTo(13f)
            verticalLineTo(8f)
            close()
            moveTo(13f, 5f)
            horizontalLineToRelative(2.85f)
            quadTo(15.2f, 4.6f, 14.48f, 4.39f)
            quadTo(13.75f, 4.17f, 13f, 4.05f)
            verticalLineTo(5f)
            close()
            moveToRelative(-2f, 7f)
            close()
          }
        }
        .build().also { _tonalityW400 = it }
        IconWeight.W500 -> _tonalityW500 ?: ImageVector.Builder(
          name = "tonality",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.02f, 21.4f)
            quadTo(6.16f, 20.6f, 4.78f, 19.22f)
            reflectiveQuadTo(2.6f, 15.98f)
            reflectiveQuadTo(1.8f, 12f)
            reflectiveQuadTo(2.6f, 8.02f)
            reflectiveQuadTo(4.78f, 4.78f)
            reflectiveQuadTo(8.02f, 2.6f)
            reflectiveQuadTo(12f, 1.8f)
            reflectiveQuadToRelative(3.98f, 0.8f)
            reflectiveQuadToRelative(3.24f, 2.18f)
            reflectiveQuadTo(21.4f, 8.02f)
            reflectiveQuadTo(22.2f, 12f)
            reflectiveQuadToRelative(-0.8f, 3.98f)
            reflectiveQuadToRelative(-2.18f, 3.24f)
            reflectiveQuadTo(15.98f, 21.4f)
            reflectiveQuadTo(12f, 22.2f)
            reflectiveQuadTo(8.02f, 21.4f)
            close()
            moveToRelative(2.84f, -1.55f)
            verticalLineTo(4.15f)
            quadTo(7.92f, 4.57f, 6f, 6.8f)
            reflectiveQuadTo(4.07f, 12f)
            reflectiveQuadTo(6f, 17.2f)
            reflectiveQuadToRelative(4.87f, 2.65f)
            close()
            moveToRelative(2.28f, 0f)
            quadToRelative(0.73f, -0.13f, 1.43f, -0.35f)
            reflectiveQuadTo(15.9f, 18.89f)
            horizontalLineTo(13.14f)
            verticalLineToRelative(0.96f)
            close()
            moveToRelative(0f, -2.92f)
            horizontalLineToRelative(5.08f)
            quadToRelative(0.2f, -0.22f, 0.35f, -0.47f)
            reflectiveQuadToRelative(0.3f, -0.52f)
            horizontalLineTo(13.14f)
            verticalLineToRelative(0.99f)
            close()
            moveToRelative(0f, -2.96f)
            horizontalLineToRelative(6.54f)
            quadToRelative(0.06f, -0.24f, 0.1f, -0.49f)
            reflectiveQuadToRelative(0.1f, -0.5f)
            horizontalLineTo(13.14f)
            verticalLineToRelative(0.99f)
            close()
            moveToRelative(0f, -2.95f)
            horizontalLineToRelative(6.74f)
            quadToRelative(-0.05f, -0.25f, -0.1f, -0.5f)
            reflectiveQuadToRelative(-0.1f, -0.49f)
            horizontalLineTo(13.14f)
            verticalLineToRelative(0.99f)
            close()
            moveToRelative(0f, -2.95f)
            horizontalLineToRelative(5.73f)
            quadTo(18.71f, 7.8f, 18.56f, 7.55f)
            reflectiveQuadTo(18.21f, 7.07f)
            horizontalLineTo(13.14f)
            verticalLineTo(8.07f)
            close()
            moveToRelative(0f, -2.96f)
            horizontalLineTo(15.9f)
            quadTo(15.27f, 4.73f, 14.56f, 4.5f)
            reflectiveQuadTo(13.14f, 4.15f)
            verticalLineTo(5.11f)
            close()
            moveTo(10.86f, 12f)
            close()
          }
        }
        .build().also { _tonalityW500 = it }
    }

private var _tonalityW300: ImageVector? = null
private var _tonalityW400: ImageVector? = null
private var _tonalityW500: ImageVector? = null

public val FujiIcons.Tonality2: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _tonality2W300 ?: ImageVector.Builder(
          name = "tonality_2",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.38f, 20.57f)
            quadTo(6.68f, 19.83f, 5.42f, 18.58f)
            reflectiveQuadTo(3.44f, 15.63f)
            reflectiveQuadTo(2.7f, 12f)
            reflectiveQuadTo(3.43f, 8.38f)
            reflectiveQuadTo(5.42f, 5.42f)
            reflectiveQuadTo(8.37f, 3.44f)
            reflectiveQuadTo(12f, 2.7f)
            reflectiveQuadToRelative(3.63f, 0.73f)
            reflectiveQuadToRelative(2.95f, 1.99f)
            reflectiveQuadToRelative(1.99f, 2.95f)
            reflectiveQuadTo(21.3f, 12f)
            reflectiveQuadToRelative(-0.73f, 3.63f)
            reflectiveQuadToRelative(-1.99f, 2.95f)
            reflectiveQuadToRelative(-2.95f, 1.99f)
            reflectiveQuadTo(12f, 21.3f)
            reflectiveQuadTo(8.38f, 20.57f)
            close()
            moveTo(12.7f, 4.13f)
            verticalLineTo(19.87f)
            quadToRelative(3.06f, -0.39f, 5.13f, -2.61f)
            reflectiveQuadTo(19.9f, 12f)
            reflectiveQuadTo(17.83f, 6.74f)
            reflectiveQuadTo(12.7f, 4.13f)
            close()
            moveTo(11.3f, 5.38f)
            verticalLineTo(4.13f)
            quadTo(10.34f, 4.21f, 9.42f, 4.5f)
            reflectiveQuadTo(7.75f, 5.38f)
            horizontalLineTo(11.3f)
            close()
            moveToRelative(0f, 2.96f)
            verticalLineTo(6.78f)
            horizontalLineTo(6.1f)
            quadTo(5.75f, 7.12f, 5.49f, 7.51f)
            reflectiveQuadTo(5.03f, 8.34f)
            horizontalLineTo(11.3f)
            close()
            moveToRelative(0f, 2.96f)
            verticalLineTo(9.74f)
            horizontalLineTo(4.43f)
            quadTo(4.3f, 10.12f, 4.23f, 10.51f)
            reflectiveQuadTo(4.13f, 11.3f)
            horizontalLineTo(11.3f)
            close()
            moveToRelative(0f, 2.96f)
            verticalLineTo(12.7f)
            horizontalLineTo(4.13f)
            quadToRelative(0.03f, 0.4f, 0.1f, 0.79f)
            reflectiveQuadToRelative(0.2f, 0.77f)
            horizontalLineTo(11.3f)
            close()
            moveToRelative(0f, 2.96f)
            verticalLineTo(15.66f)
            horizontalLineTo(5.03f)
            quadToRelative(0.2f, 0.44f, 0.46f, 0.83f)
            reflectiveQuadTo(6.1f, 17.22f)
            horizontalLineToRelative(5.2f)
            close()
            moveToRelative(0f, 2.65f)
            verticalLineTo(18.62f)
            horizontalLineTo(7.75f)
            quadTo(8.51f, 19.2f, 9.42f, 19.5f)
            reflectiveQuadToRelative(1.88f, 0.37f)
            close()
            moveTo(12.7f, 12f)
            close()
          }
        }
        .build().also { _tonality2W300 = it }
        IconWeight.W400 -> _tonality2W400 ?: ImageVector.Builder(
          name = "tonality_2",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            close()
            moveTo(13f, 4.05f)
            verticalLineToRelative(15.9f)
            quadToRelative(3.03f, -0.38f, 5.01f, -2.65f)
            reflectiveQuadTo(20f, 12f)
            reflectiveQuadTo(18.01f, 6.7f)
            quadTo(16.03f, 4.42f, 13f, 4.05f)
            close()
            moveTo(11f, 5f)
            verticalLineTo(4.05f)
            quadTo(10.25f, 4.17f, 9.53f, 4.39f)
            reflectiveQuadTo(8.15f, 5f)
            horizontalLineTo(11f)
            close()
            moveToRelative(0f, 3f)
            verticalLineTo(7f)
            horizontalLineTo(5.75f)
            quadTo(5.55f, 7.22f, 5.4f, 7.47f)
            reflectiveQuadTo(5.1f, 8f)
            horizontalLineTo(11f)
            close()
            moveToRelative(0f, 3f)
            verticalLineTo(10f)
            horizontalLineTo(4.25f)
            quadTo(4.2f, 10.25f, 4.15f, 10.5f)
            reflectiveQuadTo(4.05f, 11f)
            horizontalLineTo(11f)
            close()
            moveToRelative(0f, 3f)
            verticalLineTo(13f)
            horizontalLineTo(4.05f)
            quadToRelative(0.05f, 0.25f, 0.1f, 0.5f)
            reflectiveQuadTo(4.25f, 14f)
            horizontalLineTo(11f)
            close()
            moveToRelative(0f, 3f)
            verticalLineTo(16f)
            horizontalLineTo(5.1f)
            quadToRelative(0.15f, 0.27f, 0.3f, 0.52f)
            reflectiveQuadTo(5.75f, 17f)
            horizontalLineTo(11f)
            close()
            moveToRelative(0f, 2.95f)
            verticalLineTo(19f)
            horizontalLineTo(8.15f)
            quadToRelative(0.65f, 0.4f, 1.38f, 0.61f)
            reflectiveQuadTo(11f, 19.95f)
            close()
            moveTo(13f, 12f)
            close()
          }
        }
        .build().also { _tonality2W400 = it }
        IconWeight.W500 -> _tonality2W500 ?: ImageVector.Builder(
          name = "tonality_2",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(8.02f, 21.4f)
            quadTo(6.16f, 20.6f, 4.78f, 19.22f)
            reflectiveQuadTo(2.6f, 15.98f)
            reflectiveQuadTo(1.8f, 12f)
            reflectiveQuadTo(2.6f, 8.02f)
            reflectiveQuadTo(4.78f, 4.78f)
            reflectiveQuadTo(8.02f, 2.6f)
            reflectiveQuadTo(12f, 1.8f)
            reflectiveQuadToRelative(3.98f, 0.8f)
            reflectiveQuadToRelative(3.24f, 2.18f)
            reflectiveQuadTo(21.4f, 8.02f)
            reflectiveQuadTo(22.2f, 12f)
            reflectiveQuadToRelative(-0.8f, 3.98f)
            reflectiveQuadToRelative(-2.18f, 3.24f)
            reflectiveQuadTo(15.98f, 21.4f)
            reflectiveQuadTo(12f, 22.2f)
            reflectiveQuadTo(8.02f, 21.4f)
            close()
            moveTo(13.14f, 4.15f)
            verticalLineToRelative(15.7f)
            quadTo(16.08f, 19.43f, 18f, 17.2f)
            reflectiveQuadTo(19.93f, 12f)
            reflectiveQuadTo(18f, 6.8f)
            reflectiveQuadTo(13.14f, 4.15f)
            close()
            moveTo(10.86f, 5.11f)
            verticalLineTo(4.15f)
            quadTo(10.14f, 4.28f, 9.44f, 4.5f)
            reflectiveQuadTo(8.1f, 5.11f)
            horizontalLineToRelative(2.76f)
            close()
            moveToRelative(0f, 2.96f)
            verticalLineTo(7.07f)
            horizontalLineTo(5.79f)
            quadTo(5.59f, 7.3f, 5.44f, 7.55f)
            reflectiveQuadTo(5.14f, 8.07f)
            horizontalLineToRelative(5.73f)
            close()
            moveToRelative(0f, 2.95f)
            verticalLineTo(10.03f)
            horizontalLineTo(4.32f)
            quadToRelative(-0.06f, 0.24f, -0.1f, 0.49f)
            reflectiveQuadToRelative(-0.1f, 0.5f)
            horizontalLineToRelative(6.74f)
            close()
            moveToRelative(0f, 2.95f)
            verticalLineTo(12.98f)
            horizontalLineTo(4.12f)
            quadToRelative(0.05f, 0.25f, 0.1f, 0.5f)
            reflectiveQuadToRelative(0.1f, 0.49f)
            horizontalLineToRelative(6.54f)
            close()
            moveToRelative(0f, 2.96f)
            verticalLineTo(15.93f)
            horizontalLineTo(5.14f)
            quadToRelative(0.15f, 0.27f, 0.3f, 0.52f)
            reflectiveQuadToRelative(0.35f, 0.47f)
            horizontalLineToRelative(5.08f)
            close()
            moveToRelative(0f, 2.92f)
            verticalLineTo(18.89f)
            horizontalLineTo(8.1f)
            quadToRelative(0.63f, 0.38f, 1.33f, 0.61f)
            reflectiveQuadToRelative(1.43f, 0.35f)
            close()
            moveTo(13.14f, 12f)
            close()
          }
        }
        .build().also { _tonality2W500 = it }
    }

private var _tonality2W300: ImageVector? = null
private var _tonality2W400: ImageVector? = null
private var _tonality2W500: ImageVector? = null

public val FujiIcons.TransitionDissolve: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _transitionDissolveW300 ?: ImageVector.Builder(
          name = "transition_dissolve",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.87f, 19.3f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(4.7f, 18.13f)
            reflectiveQuadTo(5.04f, 17.3f)
            reflectiveQuadTo(5.87f, 16.96f)
            reflectiveQuadTo(6.7f, 17.3f)
            reflectiveQuadToRelative(0.34f, 0.84f)
            quadToRelative(0f, 0.49f, -0.34f, 0.83f)
            reflectiveQuadTo(5.87f, 19.3f)
            close()
            moveToRelative(4.07f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(8.77f, 18.13f)
            reflectiveQuadTo(9.11f, 17.3f)
            reflectiveQuadTo(9.95f, 16.96f)
            quadToRelative(0.49f, 0f, 0.83f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.84f)
            quadToRelative(0f, 0.49f, -0.35f, 0.83f)
            reflectiveQuadTo(9.94f, 19.3f)
            close()
            moveToRelative(4.1f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(12.88f, 18.13f)
            reflectiveQuadTo(13.22f, 17.3f)
            reflectiveQuadToRelative(0.83f, -0.34f)
            reflectiveQuadToRelative(0.83f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.84f)
            quadToRelative(0f, 0.49f, -0.34f, 0.83f)
            reflectiveQuadTo(14.04f, 19.3f)
            close()
            moveToRelative(4.09f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(16.96f, 18.13f)
            reflectiveQuadTo(17.3f, 17.3f)
            reflectiveQuadToRelative(0.83f, -0.34f)
            reflectiveQuadToRelative(0.83f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.84f)
            quadToRelative(0f, 0.49f, -0.34f, 0.83f)
            reflectiveQuadTo(18.13f, 19.3f)
            close()
            moveTo(5.87f, 15.23f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(4.7f, 14.05f)
            quadToRelative(0f, -0.49f, 0.34f, -0.83f)
            reflectiveQuadTo(5.87f, 12.88f)
            reflectiveQuadTo(6.7f, 13.22f)
            reflectiveQuadToRelative(0.34f, 0.83f)
            reflectiveQuadTo(6.7f, 14.89f)
            reflectiveQuadTo(5.87f, 15.23f)
            close()
            moveToRelative(4.07f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(8.77f, 14.05f)
            quadToRelative(0f, -0.49f, 0.34f, -0.83f)
            reflectiveQuadTo(9.95f, 12.88f)
            quadToRelative(0.49f, 0f, 0.83f, 0.35f)
            reflectiveQuadToRelative(0.34f, 0.83f)
            reflectiveQuadToRelative(-0.35f, 0.83f)
            reflectiveQuadTo(9.94f, 15.23f)
            close()
            moveToRelative(4.1f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(12.88f, 14.05f)
            quadToRelative(0f, -0.49f, 0.34f, -0.83f)
            reflectiveQuadToRelative(0.83f, -0.34f)
            reflectiveQuadToRelative(0.83f, 0.35f)
            reflectiveQuadToRelative(0.34f, 0.83f)
            reflectiveQuadToRelative(-0.34f, 0.83f)
            reflectiveQuadToRelative(-0.84f, 0.34f)
            close()
            moveToRelative(4.09f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(16.96f, 14.05f)
            quadToRelative(0f, -0.49f, 0.34f, -0.83f)
            reflectiveQuadToRelative(0.83f, -0.34f)
            reflectiveQuadToRelative(0.83f, 0.35f)
            reflectiveQuadToRelative(0.34f, 0.83f)
            reflectiveQuadToRelative(-0.34f, 0.83f)
            reflectiveQuadToRelative(-0.83f, 0.34f)
            close()
            moveTo(5.87f, 11.12f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(4.7f, 9.95f)
            reflectiveQuadTo(5.04f, 9.12f)
            reflectiveQuadTo(5.87f, 8.78f)
            reflectiveQuadTo(6.7f, 9.12f)
            reflectiveQuadTo(7.04f, 9.96f)
            reflectiveQuadTo(6.7f, 10.78f)
            reflectiveQuadTo(5.87f, 11.12f)
            close()
            moveToRelative(4.07f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(8.77f, 9.95f)
            reflectiveQuadTo(9.11f, 9.12f)
            reflectiveQuadTo(9.95f, 8.78f)
            quadToRelative(0.49f, 0f, 0.83f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.84f)
            reflectiveQuadToRelative(-0.35f, 0.83f)
            reflectiveQuadTo(9.94f, 11.12f)
            close()
            moveToRelative(4.1f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(12.88f, 9.95f)
            reflectiveQuadTo(13.22f, 9.12f)
            reflectiveQuadTo(14.05f, 8.78f)
            reflectiveQuadToRelative(0.83f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.84f)
            reflectiveQuadToRelative(-0.34f, 0.83f)
            reflectiveQuadToRelative(-0.84f, 0.34f)
            close()
            moveToRelative(4.09f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(16.96f, 9.95f)
            reflectiveQuadTo(17.3f, 9.12f)
            reflectiveQuadTo(18.12f, 8.78f)
            reflectiveQuadToRelative(0.83f, 0.34f)
            reflectiveQuadTo(19.3f, 9.96f)
            reflectiveQuadToRelative(-0.34f, 0.83f)
            reflectiveQuadToRelative(-0.83f, 0.34f)
            close()
            moveTo(5.87f, 7.04f)
            quadTo(5.38f, 7.04f, 5.04f, 6.7f)
            reflectiveQuadTo(4.7f, 5.88f)
            reflectiveQuadTo(5.04f, 5.04f)
            reflectiveQuadTo(5.87f, 4.7f)
            reflectiveQuadTo(6.7f, 5.04f)
            reflectiveQuadTo(7.04f, 5.87f)
            reflectiveQuadTo(6.7f, 6.7f)
            reflectiveQuadTo(5.87f, 7.04f)
            close()
            moveToRelative(4.07f, 0f)
            quadTo(9.46f, 7.04f, 9.11f, 6.7f)
            reflectiveQuadTo(8.77f, 5.88f)
            reflectiveQuadTo(9.11f, 5.04f)
            reflectiveQuadTo(9.95f, 4.7f)
            quadToRelative(0.49f, 0f, 0.83f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.83f)
            reflectiveQuadTo(10.78f, 6.7f)
            reflectiveQuadTo(9.94f, 7.04f)
            close()
            moveToRelative(4.1f, 0f)
            quadToRelative(-0.49f, 0f, -0.83f, -0.34f)
            reflectiveQuadTo(12.88f, 5.88f)
            reflectiveQuadTo(13.22f, 5.04f)
            reflectiveQuadTo(14.05f, 4.7f)
            reflectiveQuadToRelative(0.83f, 0.34f)
            reflectiveQuadToRelative(0.34f, 0.83f)
            reflectiveQuadTo(14.88f, 6.7f)
            reflectiveQuadTo(14.04f, 7.04f)
            close()
            moveToRelative(4.09f, 0f)
            quadTo(17.64f, 7.04f, 17.3f, 6.7f)
            reflectiveQuadTo(16.96f, 5.88f)
            reflectiveQuadTo(17.3f, 5.04f)
            reflectiveQuadTo(18.12f, 4.7f)
            reflectiveQuadToRelative(0.83f, 0.34f)
            reflectiveQuadTo(19.3f, 5.87f)
            reflectiveQuadTo(18.96f, 6.7f)
            reflectiveQuadTo(18.13f, 7.04f)
            close()
          }
        }
        .build().also { _transitionDissolveW300 = it }
        IconWeight.W400 -> _transitionDissolveW400 ?: ImageVector.Builder(
          name = "transition_dissolve",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.5f, 20f)
            quadTo(4.88f, 20f, 4.44f, 19.56f)
            reflectiveQuadTo(4f, 18.5f)
            reflectiveQuadTo(4.44f, 17.44f)
            reflectiveQuadTo(5.5f, 17f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(7f, 18.5f)
            reflectiveQuadTo(6.56f, 19.56f)
            reflectiveQuadTo(5.5f, 20f)
            close()
            moveToRelative(4.33f, 0f)
            quadTo(9.2f, 20f, 8.76f, 19.56f)
            reflectiveQuadTo(8.33f, 18.5f)
            reflectiveQuadTo(8.76f, 17.44f)
            reflectiveQuadTo(9.83f, 17f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(9.83f, 20f)
            close()
            moveToRelative(4.35f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(12.68f, 18.5f)
            reflectiveQuadToRelative(0.44f, -1.06f)
            reflectiveQuadTo(14.18f, 17f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(14.18f, 20f)
            close()
            moveToRelative(4.32f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(17f, 18.5f)
            reflectiveQuadToRelative(0.44f, -1.06f)
            reflectiveQuadTo(18.5f, 17f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(20f, 18.5f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(18.5f, 20f)
            close()
            moveTo(5.5f, 15.68f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(4f, 14.18f)
            reflectiveQuadTo(4.44f, 13.11f)
            reflectiveQuadTo(5.5f, 12.68f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(7f, 14.18f)
            reflectiveQuadTo(6.56f, 15.24f)
            reflectiveQuadTo(5.5f, 15.68f)
            close()
            moveToRelative(4.33f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(8.33f, 14.18f)
            reflectiveQuadTo(8.76f, 13.11f)
            reflectiveQuadTo(9.83f, 12.68f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(9.83f, 15.68f)
            close()
            moveToRelative(4.35f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(12.68f, 14.18f)
            reflectiveQuadToRelative(0.44f, -1.06f)
            reflectiveQuadToRelative(1.06f, -0.44f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadToRelative(-1.06f, 0.44f)
            close()
            moveToRelative(4.32f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(17f, 14.18f)
            reflectiveQuadToRelative(0.44f, -1.06f)
            reflectiveQuadTo(18.5f, 12.68f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(20f, 14.18f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(18.5f, 15.68f)
            close()
            moveTo(5.5f, 11.33f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(4f, 9.82f)
            reflectiveQuadTo(4.44f, 8.76f)
            reflectiveQuadTo(5.5f, 8.32f)
            reflectiveQuadTo(6.56f, 8.76f)
            reflectiveQuadTo(7f, 9.82f)
            reflectiveQuadTo(6.56f, 10.89f)
            reflectiveQuadTo(5.5f, 11.33f)
            close()
            moveToRelative(4.33f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(8.33f, 9.82f)
            reflectiveQuadTo(8.76f, 8.76f)
            reflectiveQuadTo(9.83f, 8.32f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(9.83f, 11.33f)
            close()
            moveToRelative(4.35f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(12.68f, 9.82f)
            reflectiveQuadTo(13.11f, 8.76f)
            reflectiveQuadTo(14.18f, 8.32f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadToRelative(0.44f, 1.06f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadToRelative(-1.06f, 0.44f)
            close()
            moveToRelative(4.32f, 0f)
            quadToRelative(-0.63f, 0f, -1.06f, -0.44f)
            reflectiveQuadTo(17f, 9.82f)
            reflectiveQuadTo(17.44f, 8.76f)
            reflectiveQuadTo(18.5f, 8.32f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(20f, 9.82f)
            reflectiveQuadToRelative(-0.44f, 1.06f)
            reflectiveQuadTo(18.5f, 11.33f)
            close()
            moveTo(5.5f, 7f)
            quadTo(4.88f, 7f, 4.44f, 6.56f)
            reflectiveQuadTo(4f, 5.5f)
            reflectiveQuadTo(4.44f, 4.44f)
            reflectiveQuadTo(5.5f, 4f)
            reflectiveQuadTo(6.56f, 4.44f)
            reflectiveQuadTo(7f, 5.5f)
            reflectiveQuadTo(6.56f, 6.56f)
            reflectiveQuadTo(5.5f, 7f)
            close()
            moveTo(9.83f, 7f)
            quadTo(9.2f, 7f, 8.76f, 6.56f)
            reflectiveQuadTo(8.33f, 5.5f)
            reflectiveQuadTo(8.76f, 4.44f)
            reflectiveQuadTo(9.83f, 4f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(11.33f, 5.5f)
            reflectiveQuadTo(10.89f, 6.56f)
            reflectiveQuadTo(9.83f, 7f)
            close()
            moveToRelative(4.35f, 0f)
            quadTo(13.55f, 7f, 13.11f, 6.56f)
            reflectiveQuadTo(12.68f, 5.5f)
            reflectiveQuadTo(13.11f, 4.44f)
            reflectiveQuadTo(14.18f, 4f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(15.68f, 5.5f)
            reflectiveQuadTo(15.24f, 6.56f)
            reflectiveQuadTo(14.18f, 7f)
            close()
            moveTo(18.5f, 7f)
            quadTo(17.88f, 7f, 17.44f, 6.56f)
            reflectiveQuadTo(17f, 5.5f)
            reflectiveQuadTo(17.44f, 4.44f)
            reflectiveQuadTo(18.5f, 4f)
            reflectiveQuadToRelative(1.06f, 0.44f)
            reflectiveQuadTo(20f, 5.5f)
            reflectiveQuadTo(19.56f, 6.56f)
            reflectiveQuadTo(18.5f, 7f)
            close()
          }
        }
        .build().also { _transitionDissolveW400 = it }
        IconWeight.W500 -> _transitionDissolveW500 ?: ImageVector.Builder(
          name = "transition_dissolve",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5.38f, 20.2f)
            quadToRelative(-0.66f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(3.8f, 18.62f)
            reflectiveQuadTo(4.26f, 17.5f)
            reflectiveQuadTo(5.38f, 17.04f)
            reflectiveQuadTo(6.5f, 17.5f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadTo(6.5f, 19.74f)
            reflectiveQuadTo(5.38f, 20.2f)
            close()
            moveToRelative(4.41f, 0f)
            quadToRelative(-0.66f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(8.21f, 18.62f)
            reflectiveQuadTo(8.67f, 17.5f)
            reflectiveQuadTo(9.79f, 17.04f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadToRelative(-0.46f, 1.12f)
            reflectiveQuadTo(9.79f, 20.2f)
            close()
            moveToRelative(4.43f, 0f)
            quadToRelative(-0.65f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(12.63f, 18.62f)
            reflectiveQuadTo(13.1f, 17.5f)
            reflectiveQuadToRelative(1.12f, -0.46f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadToRelative(-0.46f, 1.12f)
            reflectiveQuadTo(14.22f, 20.2f)
            close()
            moveToRelative(4.4f, 0f)
            quadToRelative(-0.65f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(17.04f, 18.62f)
            reflectiveQuadTo(17.5f, 17.5f)
            reflectiveQuadToRelative(1.12f, -0.46f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadToRelative(-0.46f, 1.12f)
            reflectiveQuadTo(18.62f, 20.2f)
            close()
            moveTo(5.38f, 15.79f)
            quadToRelative(-0.66f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(3.8f, 14.21f)
            reflectiveQuadTo(4.26f, 13.09f)
            reflectiveQuadTo(5.38f, 12.63f)
            reflectiveQuadTo(6.5f, 13.09f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadTo(6.5f, 15.33f)
            reflectiveQuadTo(5.38f, 15.79f)
            close()
            moveToRelative(4.41f, 0f)
            quadToRelative(-0.66f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(8.21f, 14.21f)
            reflectiveQuadTo(8.67f, 13.09f)
            reflectiveQuadTo(9.79f, 12.63f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadToRelative(-0.46f, 1.12f)
            reflectiveQuadTo(9.79f, 15.79f)
            close()
            moveToRelative(4.43f, 0f)
            quadToRelative(-0.65f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(12.63f, 14.21f)
            reflectiveQuadTo(13.1f, 13.09f)
            reflectiveQuadToRelative(1.12f, -0.46f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadToRelative(-0.46f, 1.12f)
            reflectiveQuadToRelative(-1.12f, 0.46f)
            close()
            moveToRelative(4.4f, 0f)
            quadToRelative(-0.65f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(17.04f, 14.21f)
            reflectiveQuadTo(17.5f, 13.09f)
            reflectiveQuadToRelative(1.12f, -0.46f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadToRelative(-0.46f, 1.12f)
            reflectiveQuadToRelative(-1.12f, 0.46f)
            close()
            moveTo(5.38f, 11.37f)
            quadToRelative(-0.66f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(3.8f, 9.78f)
            reflectiveQuadTo(4.26f, 8.66f)
            reflectiveQuadTo(5.38f, 8.2f)
            reflectiveQuadTo(6.5f, 8.66f)
            reflectiveQuadTo(6.96f, 9.78f)
            reflectiveQuadTo(6.5f, 10.9f)
            reflectiveQuadTo(5.38f, 11.37f)
            close()
            moveToRelative(4.41f, 0f)
            quadToRelative(-0.66f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(8.21f, 9.78f)
            reflectiveQuadTo(8.67f, 8.66f)
            reflectiveQuadTo(9.79f, 8.2f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadTo(10.91f, 10.9f)
            reflectiveQuadTo(9.79f, 11.37f)
            close()
            moveToRelative(4.43f, 0f)
            quadToRelative(-0.65f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(12.63f, 9.78f)
            reflectiveQuadTo(13.1f, 8.66f)
            reflectiveQuadTo(14.22f, 8.2f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadTo(15.8f, 9.78f)
            reflectiveQuadTo(15.34f, 10.9f)
            reflectiveQuadToRelative(-1.12f, 0.46f)
            close()
            moveToRelative(4.4f, 0f)
            quadToRelative(-0.65f, 0f, -1.12f, -0.46f)
            reflectiveQuadTo(17.04f, 9.78f)
            reflectiveQuadTo(17.5f, 8.66f)
            reflectiveQuadTo(18.62f, 8.2f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadTo(20.2f, 9.78f)
            reflectiveQuadTo(19.74f, 10.9f)
            reflectiveQuadToRelative(-1.12f, 0.46f)
            close()
            moveTo(5.38f, 6.96f)
            quadTo(4.72f, 6.96f, 4.26f, 6.5f)
            reflectiveQuadTo(3.8f, 5.38f)
            reflectiveQuadTo(4.26f, 4.26f)
            reflectiveQuadTo(5.38f, 3.8f)
            reflectiveQuadTo(6.5f, 4.26f)
            reflectiveQuadTo(6.96f, 5.38f)
            reflectiveQuadTo(6.5f, 6.5f)
            reflectiveQuadTo(5.38f, 6.96f)
            close()
            moveToRelative(4.41f, 0f)
            quadTo(9.13f, 6.96f, 8.67f, 6.5f)
            reflectiveQuadTo(8.21f, 5.38f)
            reflectiveQuadTo(8.67f, 4.26f)
            reflectiveQuadTo(9.79f, 3.8f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadToRelative(0.46f, 1.12f)
            reflectiveQuadTo(10.91f, 6.5f)
            reflectiveQuadTo(9.79f, 6.96f)
            close()
            moveToRelative(4.43f, 0f)
            quadTo(13.56f, 6.96f, 13.1f, 6.5f)
            reflectiveQuadTo(12.63f, 5.38f)
            reflectiveQuadTo(13.1f, 4.26f)
            reflectiveQuadTo(14.22f, 3.8f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadTo(15.8f, 5.38f)
            reflectiveQuadTo(15.34f, 6.5f)
            reflectiveQuadTo(14.22f, 6.96f)
            close()
            moveToRelative(4.4f, 0f)
            quadTo(17.96f, 6.96f, 17.5f, 6.5f)
            reflectiveQuadTo(17.04f, 5.38f)
            reflectiveQuadTo(17.5f, 4.26f)
            reflectiveQuadTo(18.62f, 3.8f)
            reflectiveQuadToRelative(1.12f, 0.46f)
            reflectiveQuadTo(20.2f, 5.38f)
            reflectiveQuadTo(19.74f, 6.5f)
            reflectiveQuadTo(18.62f, 6.96f)
            close()
          }
        }
        .build().also { _transitionDissolveW500 = it }
    }

private var _transitionDissolveW300: ImageVector? = null
private var _transitionDissolveW400: ImageVector? = null
private var _transitionDissolveW500: ImageVector? = null

public val FujiIcons.Tune: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _tuneW300 ?: ImageVector.Builder(
          name = "tune",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.43f, 20.38f)
            quadToRelative(-0.2f, -0.21f, -0.2f, -0.5f)
            verticalLineTo(15.94f)
            quadToRelative(0f, -0.29f, 0.2f, -0.5f)
            reflectiveQuadToRelative(0.49f, -0.21f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.5f)
            verticalLineToRelative(1.27f)
            horizontalLineTo(19.9f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            reflectiveQuadTo(20.4f, 18.4f)
            reflectiveQuadToRelative(-0.5f, 0.21f)
            horizontalLineTo(12.62f)
            verticalLineToRelative(1.28f)
            quadToRelative(0f, 0.29f, -0.21f, 0.5f)
            reflectiveQuadToRelative(-0.5f, 0.21f)
            reflectiveQuadTo(11.43f, 20.38f)
            close()
            moveTo(4.09f, 18.61f)
            quadTo(3.8f, 18.61f, 3.6f, 18.4f)
            reflectiveQuadTo(3.39f, 17.91f)
            reflectiveQuadTo(3.6f, 17.41f)
            reflectiveQuadToRelative(0.5f, -0.21f)
            horizontalLineTo(8.12f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            reflectiveQuadTo(8.62f, 18.4f)
            reflectiveQuadToRelative(-0.5f, 0.21f)
            horizontalLineTo(4.09f)
            close()
            moveTo(7.63f, 14.45f)
            quadToRelative(-0.2f, -0.2f, -0.2f, -0.49f)
            verticalLineTo(12.69f)
            horizontalLineTo(4.09f)
            quadToRelative(-0.29f, 0f, -0.5f, -0.21f)
            reflectiveQuadTo(3.39f, 11.99f)
            reflectiveQuadTo(3.6f, 11.5f)
            reflectiveQuadToRelative(0.5f, -0.21f)
            horizontalLineTo(7.42f)
            verticalLineTo(10.01f)
            quadToRelative(0f, -0.29f, 0.2f, -0.49f)
            reflectiveQuadTo(8.12f, 9.31f)
            reflectiveQuadToRelative(0.5f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            verticalLineToRelative(3.95f)
            quadToRelative(0f, 0.29f, -0.21f, 0.49f)
            reflectiveQuadToRelative(-0.5f, 0.2f)
            reflectiveQuadTo(7.63f, 14.45f)
            close()
            moveToRelative(4.29f, -1.76f)
            quadToRelative(-0.29f, 0f, -0.49f, -0.21f)
            reflectiveQuadToRelative(-0.2f, -0.49f)
            reflectiveQuadToRelative(0.2f, -0.49f)
            reflectiveQuadToRelative(0.49f, -0.21f)
            horizontalLineTo(19.9f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.49f)
            reflectiveQuadTo(20.4f, 12.48f)
            reflectiveQuadToRelative(-0.5f, 0.21f)
            horizontalLineTo(11.92f)
            close()
            moveTo(15.37f, 8.52f)
            quadTo(15.17f, 8.32f, 15.17f, 8.03f)
            verticalLineTo(4.08f)
            quadToRelative(0f, -0.29f, 0.21f, -0.5f)
            reflectiveQuadTo(15.87f, 3.38f)
            reflectiveQuadToRelative(0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.5f)
            verticalLineTo(5.36f)
            horizontalLineTo(19.9f)
            quadToRelative(0.29f, 0f, 0.5f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.5f)
            reflectiveQuadTo(20.4f, 6.55f)
            reflectiveQuadToRelative(-0.5f, 0.2f)
            horizontalLineTo(16.57f)
            verticalLineTo(8.03f)
            quadToRelative(0f, 0.29f, -0.21f, 0.5f)
            reflectiveQuadTo(15.87f, 8.73f)
            reflectiveQuadTo(15.37f, 8.52f)
            close()
            moveTo(4.09f, 6.76f)
            quadTo(3.8f, 6.76f, 3.6f, 6.55f)
            reflectiveQuadTo(3.39f, 6.06f)
            reflectiveQuadTo(3.6f, 5.56f)
            reflectiveQuadTo(4.09f, 5.36f)
            horizontalLineToRelative(7.98f)
            quadToRelative(0.29f, 0f, 0.49f, 0.21f)
            reflectiveQuadToRelative(0.21f, 0.5f)
            reflectiveQuadTo(12.56f, 6.55f)
            reflectiveQuadToRelative(-0.49f, 0.2f)
            horizontalLineTo(4.09f)
            close()
          }
        }
        .build().also { _tuneW300 = it }
        IconWeight.W400 -> _tuneW400 ?: ImageVector.Builder(
          name = "tune",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.29f, 20.71f)
            quadTo(11f, 20.43f, 11f, 20f)
            verticalLineTo(16f)
            quadToRelative(0f, -0.43f, 0.29f, -0.71f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 16f)
            verticalLineToRelative(1f)
            horizontalLineToRelative(7f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 18f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(20f, 19f)
            horizontalLineTo(13f)
            verticalLineToRelative(1f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(12f, 21f)
            reflectiveQuadTo(11.29f, 20.71f)
            close()
            moveTo(4f, 19f)
            quadTo(3.58f, 19f, 3.29f, 18.71f)
            quadTo(3f, 18.43f, 3f, 18f)
            reflectiveQuadTo(3.29f, 17.29f)
            reflectiveQuadTo(4f, 17f)
            horizontalLineTo(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(9f, 18f)
            reflectiveQuadTo(8.71f, 18.71f)
            reflectiveQuadTo(8f, 19f)
            horizontalLineTo(4f)
            close()
            moveTo(7.29f, 14.71f)
            quadTo(7f, 14.43f, 7f, 14f)
            verticalLineTo(13f)
            horizontalLineTo(4f)
            quadTo(3.58f, 13f, 3.29f, 12.71f)
            quadTo(3f, 12.43f, 3f, 12f)
            reflectiveQuadTo(3.29f, 11.29f)
            reflectiveQuadTo(4f, 11f)
            horizontalLineTo(7f)
            verticalLineTo(10f)
            quadTo(7f, 9.57f, 7.29f, 9.29f)
            reflectiveQuadTo(8f, 9f)
            reflectiveQuadTo(8.71f, 9.29f)
            reflectiveQuadTo(9f, 10f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(8f, 15f)
            quadTo(7.58f, 15f, 7.29f, 14.71f)
            close()
            moveTo(12f, 13f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(11f, 12.43f, 11f, 12f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(12f, 11f)
            horizontalLineToRelative(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 12f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(20f, 13f)
            horizontalLineTo(12f)
            close()
            moveTo(15.29f, 8.71f)
            quadTo(15f, 8.42f, 15f, 8f)
            verticalLineTo(4f)
            quadTo(15f, 3.57f, 15.29f, 3.29f)
            reflectiveQuadTo(16f, 3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 4f)
            verticalLineTo(5f)
            horizontalLineToRelative(3f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(21f, 6f)
            reflectiveQuadTo(20.71f, 6.71f)
            reflectiveQuadTo(20f, 7f)
            horizontalLineTo(17f)
            verticalLineTo(8f)
            quadToRelative(0f, 0.42f, -0.29f, 0.71f)
            reflectiveQuadTo(16f, 9f)
            reflectiveQuadTo(15.29f, 8.71f)
            close()
            moveTo(4f, 7f)
            quadTo(3.58f, 7f, 3.29f, 6.71f)
            quadTo(3f, 6.43f, 3f, 6f)
            reflectiveQuadTo(3.29f, 5.29f)
            reflectiveQuadTo(4f, 5f)
            horizontalLineToRelative(8f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(13f, 6f)
            reflectiveQuadTo(12.71f, 6.71f)
            reflectiveQuadTo(12f, 7f)
            horizontalLineTo(4f)
            close()
          }
        }
        .build().also { _tuneW400 = it }
        IconWeight.W500 -> _tuneW500 ?: ImageVector.Builder(
          name = "tune",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11.27f, 20.87f)
            quadTo(10.95f, 20.56f, 10.95f, 20.1f)
            verticalLineTo(16.09f)
            quadToRelative(0f, -0.46f, 0.32f, -0.78f)
            reflectiveQuadTo(12.05f, 15f)
            reflectiveQuadToRelative(0.78f, 0.31f)
            reflectiveQuadToRelative(0.31f, 0.78f)
            verticalLineTo(17f)
            horizontalLineToRelative(6.91f)
            quadToRelative(0.46f, 0f, 0.78f, 0.31f)
            reflectiveQuadToRelative(0.31f, 0.78f)
            reflectiveQuadToRelative(-0.31f, 0.78f)
            reflectiveQuadToRelative(-0.78f, 0.32f)
            horizontalLineTo(13.14f)
            verticalLineTo(20.1f)
            quadToRelative(0f, 0.46f, -0.31f, 0.78f)
            reflectiveQuadToRelative(-0.78f, 0.31f)
            reflectiveQuadTo(11.27f, 20.87f)
            close()
            moveTo(3.95f, 19.19f)
            quadToRelative(-0.46f, 0f, -0.78f, -0.32f)
            reflectiveQuadTo(2.86f, 18.09f)
            reflectiveQuadTo(3.18f, 17.31f)
            reflectiveQuadTo(3.95f, 17f)
            horizontalLineTo(7.96f)
            quadToRelative(0.46f, 0f, 0.78f, 0.31f)
            reflectiveQuadToRelative(0.31f, 0.78f)
            reflectiveQuadTo(8.73f, 18.87f)
            reflectiveQuadTo(7.96f, 19.19f)
            horizontalLineTo(3.95f)
            close()
            moveTo(7.18f, 14.77f)
            quadTo(6.86f, 14.45f, 6.86f, 13.99f)
            verticalLineToRelative(-0.9f)
            horizontalLineTo(3.95f)
            quadToRelative(-0.46f, 0f, -0.78f, -0.31f)
            reflectiveQuadTo(2.86f, 12f)
            reflectiveQuadTo(3.18f, 11.22f)
            reflectiveQuadTo(3.95f, 10.91f)
            horizontalLineTo(6.86f)
            verticalLineTo(10f)
            quadToRelative(0f, -0.46f, 0.32f, -0.78f)
            reflectiveQuadTo(7.96f, 8.91f)
            reflectiveQuadTo(8.73f, 9.22f)
            reflectiveQuadTo(9.05f, 10f)
            verticalLineToRelative(4f)
            quadToRelative(0f, 0.46f, -0.31f, 0.78f)
            reflectiveQuadTo(7.96f, 15.09f)
            reflectiveQuadTo(7.18f, 14.77f)
            close()
            moveToRelative(4.86f, -1.68f)
            quadToRelative(-0.46f, 0f, -0.78f, -0.31f)
            reflectiveQuadTo(10.95f, 12f)
            reflectiveQuadToRelative(0.31f, -0.78f)
            reflectiveQuadToRelative(0.78f, -0.31f)
            horizontalLineToRelative(8.01f)
            quadToRelative(0.46f, 0f, 0.78f, 0.31f)
            reflectiveQuadTo(21.14f, 12f)
            reflectiveQuadToRelative(-0.31f, 0.78f)
            reflectiveQuadToRelative(-0.78f, 0.31f)
            horizontalLineTo(12.04f)
            close()
            moveToRelative(3.23f, -4.4f)
            quadTo(14.95f, 8.37f, 14.95f, 7.91f)
            verticalLineTo(3.9f)
            quadToRelative(0f, -0.46f, 0.32f, -0.78f)
            reflectiveQuadTo(16.05f, 2.81f)
            reflectiveQuadToRelative(0.78f, 0.31f)
            reflectiveQuadTo(17.14f, 3.9f)
            verticalLineTo(4.81f)
            horizontalLineToRelative(2.91f)
            quadToRelative(0.46f, 0f, 0.78f, 0.31f)
            reflectiveQuadTo(21.14f, 5.9f)
            reflectiveQuadTo(20.82f, 6.68f)
            reflectiveQuadTo(20.05f, 7f)
            horizontalLineTo(17.14f)
            verticalLineTo(7.91f)
            quadToRelative(0f, 0.46f, -0.31f, 0.78f)
            reflectiveQuadTo(16.05f, 9f)
            reflectiveQuadTo(15.27f, 8.69f)
            close()
            moveTo(3.95f, 7f)
            quadTo(3.49f, 7f, 3.18f, 6.68f)
            reflectiveQuadTo(2.86f, 5.9f)
            reflectiveQuadTo(3.18f, 5.13f)
            reflectiveQuadTo(3.95f, 4.81f)
            horizontalLineToRelative(8.01f)
            quadToRelative(0.46f, 0f, 0.78f, 0.31f)
            reflectiveQuadTo(13.05f, 5.9f)
            reflectiveQuadTo(12.73f, 6.68f)
            reflectiveQuadTo(11.96f, 7f)
            horizontalLineTo(3.95f)
            close()
          }
        }
        .build().also { _tuneW500 = it }
    }

private var _tuneW300: ImageVector? = null
private var _tuneW400: ImageVector? = null
private var _tuneW500: ImageVector? = null

public val FujiIcons.Warning: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _warningW300 ?: ImageVector.Builder(
          name = "warning",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(3.61f, 20.32f)
            quadToRelative(-0.24f, 0f, -0.44f, -0.12f)
            reflectiveQuadTo(2.87f, 19.89f)
            quadTo(2.75f, 19.7f, 2.74f, 19.48f)
            reflectiveQuadTo(2.87f, 19.03f)
            lineTo(11.27f, 4.56f)
            quadTo(11.39f, 4.34f, 11.59f, 4.24f)
            reflectiveQuadTo(12f, 4.13f)
            reflectiveQuadToRelative(0.41f, 0.1f)
            reflectiveQuadToRelative(0.33f, 0.32f)
            lineToRelative(8.4f, 14.47f)
            quadToRelative(0.13f, 0.22f, 0.12f, 0.44f)
            reflectiveQuadToRelative(-0.12f, 0.41f)
            reflectiveQuadTo(20.83f, 20.2f)
            reflectiveQuadToRelative(-0.44f, 0.12f)
            horizontalLineTo(3.61f)
            close()
            moveToRelative(8.94f, -2.83f)
            quadToRelative(0.22f, -0.22f, 0.22f, -0.55f)
            reflectiveQuadTo(12.55f, 16.39f)
            reflectiveQuadTo(12f, 16.17f)
            reflectiveQuadToRelative(-0.55f, 0.22f)
            reflectiveQuadToRelative(-0.22f, 0.55f)
            reflectiveQuadToRelative(0.22f, 0.55f)
            reflectiveQuadTo(12f, 17.71f)
            reflectiveQuadToRelative(0.55f, -0.22f)
            close()
            moveTo(12.5f, 14.99f)
            quadToRelative(0.21f, -0.21f, 0.21f, -0.5f)
            verticalLineTo(10.95f)
            quadToRelative(0f, -0.29f, -0.21f, -0.49f)
            reflectiveQuadTo(12.01f, 10.25f)
            reflectiveQuadToRelative(-0.49f, 0.21f)
            reflectiveQuadToRelative(-0.21f, 0.49f)
            verticalLineToRelative(3.54f)
            quadToRelative(0f, 0.29f, 0.21f, 0.5f)
            reflectiveQuadToRelative(0.49f, 0.21f)
            reflectiveQuadTo(12.5f, 14.99f)
            close()
          }
        }
        .build().also { _warningW300 = it }
        IconWeight.W400 -> _warningW400 ?: ImageVector.Builder(
          name = "warning",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(2.73f, 21f)
            quadTo(2.45f, 21f, 2.23f, 20.86f)
            reflectiveQuadTo(1.88f, 20.5f)
            reflectiveQuadTo(1.74f, 20.01f)
            reflectiveQuadTo(1.88f, 19.5f)
            lineToRelative(9.25f, -16f)
            quadTo(11.28f, 3.25f, 11.51f, 3.13f)
            reflectiveQuadTo(12f, 3f)
            reflectiveQuadToRelative(0.49f, 0.13f)
            reflectiveQuadTo(12.88f, 3.5f)
            lineToRelative(9.25f, 16f)
            quadToRelative(0.15f, 0.25f, 0.14f, 0.51f)
            reflectiveQuadTo(22.13f, 20.5f)
            reflectiveQuadToRelative(-0.35f, 0.36f)
            reflectiveQuadTo(21.28f, 21f)
            horizontalLineTo(2.73f)
            close()
            moveToRelative(9.99f, -3.29f)
            quadTo(13f, 17.43f, 13f, 17f)
            reflectiveQuadTo(12.71f, 16.29f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(11f, 17f)
            reflectiveQuadToRelative(0.29f, 0.71f)
            reflectiveQuadTo(12f, 18f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
            moveToRelative(0f, -3f)
            quadTo(13f, 14.43f, 13f, 14f)
            verticalLineTo(11f)
            quadToRelative(0f, -0.43f, -0.29f, -0.71f)
            reflectiveQuadTo(12f, 10f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(11f, 11f)
            verticalLineToRelative(3f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
          }
        }
        .build().also { _warningW400 = it }
        IconWeight.W500 -> _warningW500 ?: ImageVector.Builder(
          name = "warning",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(2.79f, 21.1f)
            quadToRelative(-0.32f, 0f, -0.57f, -0.16f)
            reflectiveQuadTo(1.82f, 20.53f)
            reflectiveQuadTo(1.67f, 19.98f)
            reflectiveQuadTo(1.82f, 19.39f)
            lineTo(11.01f, 3.5f)
            quadTo(11.17f, 3.21f, 11.44f, 3.07f)
            reflectiveQuadTo(12f, 2.93f)
            reflectiveQuadToRelative(0.56f, 0.14f)
            reflectiveQuadTo(12.99f, 3.5f)
            lineToRelative(9.18f, 15.89f)
            quadToRelative(0.17f, 0.29f, 0.16f, 0.58f)
            reflectiveQuadToRelative(-0.16f, 0.55f)
            reflectiveQuadToRelative(-0.4f, 0.41f)
            reflectiveQuadTo(21.21f, 21.1f)
            horizontalLineTo(2.79f)
            close()
            moveToRelative(9.94f, -3.42f)
            quadToRelative(0.3f, -0.3f, 0.3f, -0.73f)
            reflectiveQuadToRelative(-0.3f, -0.73f)
            reflectiveQuadTo(12f, 15.93f)
            reflectiveQuadToRelative(-0.73f, 0.3f)
            reflectiveQuadToRelative(-0.3f, 0.73f)
            reflectiveQuadToRelative(0.3f, 0.73f)
            reflectiveQuadTo(12f, 17.98f)
            reflectiveQuadToRelative(0.73f, -0.3f)
            close()
            moveTo(12.71f, 14.71f)
            quadTo(13f, 14.43f, 13f, 14f)
            verticalLineTo(11.07f)
            quadToRelative(0f, -0.42f, -0.29f, -0.71f)
            reflectiveQuadTo(12f, 10.07f)
            reflectiveQuadToRelative(-0.71f, 0.29f)
            reflectiveQuadTo(11f, 11.07f)
            verticalLineTo(14f)
            quadToRelative(0f, 0.42f, 0.29f, 0.71f)
            reflectiveQuadTo(12f, 15f)
            reflectiveQuadToRelative(0.71f, -0.29f)
            close()
          }
        }
        .build().also { _warningW500 = it }
    }

private var _warningW300: ImageVector? = null
private var _warningW400: ImageVector? = null
private var _warningW500: ImageVector? = null

public val FujiIcons.WbAuto: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _wbAutoW300 ?: ImageVector.Builder(
          name = "wb_auto",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6.89f, 13.06f)
            horizontalLineTo(9.67f)
            lineToRelative(0.58f, 1.59f)
            quadToRelative(0.05f, 0.14f, 0.16f, 0.22f)
            reflectiveQuadToRelative(0.27f, 0.08f)
            quadToRelative(0.26f, 0f, 0.4f, -0.2f)
            reflectiveQuadToRelative(0.05f, -0.44f)
            lineTo(8.85f, 8.31f)
            quadTo(8.8f, 8.19f, 8.69f, 8.11f)
            reflectiveQuadTo(8.45f, 8.04f)
            horizontalLineTo(8.13f)
            quadTo(8f, 8.04f, 7.89f, 8.11f)
            reflectiveQuadTo(7.73f, 8.31f)
            lineTo(5.46f, 14.34f)
            quadToRelative(-0.09f, 0.23f, 0.05f, 0.42f)
            reflectiveQuadToRelative(0.38f, 0.19f)
            quadToRelative(0.16f, 0f, 0.27f, -0.08f)
            reflectiveQuadTo(6.33f, 14.65f)
            lineTo(6.89f, 13.06f)
            close()
            moveTo(7.2f, 12.26f)
            lineTo(8.24f, 9.32f)
            horizontalLineToRelative(0.1f)
            lineToRelative(1.02f, 2.94f)
            horizontalLineTo(7.2f)
            close()
            moveToRelative(1.09f, 6.08f)
            quadToRelative(-2.65f, 0f, -4.5f, -1.84f)
            reflectiveQuadTo(1.95f, 12f)
            reflectiveQuadTo(3.8f, 7.51f)
            reflectiveQuadTo(8.29f, 5.66f)
            quadToRelative(1.91f, 0f, 3.48f, 1.06f)
            reflectiveQuadToRelative(2.28f, 2.81f)
            lineTo(13.82f, 8.65f)
            quadTo(13.76f, 8.42f, 13.9f, 8.23f)
            reflectiveQuadTo(14.28f, 8.04f)
            quadToRelative(0.18f, 0f, 0.29f, 0.1f)
            reflectiveQuadTo(14.74f, 8.4f)
            lineToRelative(1.13f, 4.89f)
            horizontalLineToRelative(0.05f)
            lineTo(17.26f, 8.38f)
            quadTo(17.3f, 8.22f, 17.42f, 8.13f)
            reflectiveQuadTo(17.7f, 8.04f)
            horizontalLineToRelative(0.26f)
            quadToRelative(0.15f, 0f, 0.27f, 0.09f)
            reflectiveQuadToRelative(0.16f, 0.25f)
            lineToRelative(1.34f, 4.92f)
            horizontalLineTo(19.8f)
            lineTo(20.93f, 8.41f)
            quadTo(20.98f, 8.25f, 21.11f, 8.14f)
            reflectiveQuadToRelative(0.3f, -0.1f)
            quadToRelative(0.24f, 0f, 0.38f, 0.19f)
            reflectiveQuadToRelative(0.09f, 0.43f)
            lineToRelative(-1.52f, 5.96f)
            quadToRelative(-0.05f, 0.17f, -0.17f, 0.26f)
            reflectiveQuadToRelative(-0.28f, 0.09f)
            horizontalLineTo(19.67f)
            quadToRelative(-0.16f, 0f, -0.28f, -0.1f)
            reflectiveQuadTo(19.23f, 14.62f)
            lineTo(17.84f, 9.54f)
            horizontalLineTo(17.79f)
            lineToRelative(-1.37f, 5.09f)
            quadToRelative(-0.05f, 0.16f, -0.16f, 0.25f)
            reflectiveQuadToRelative(-0.27f, 0.09f)
            horizontalLineTo(15.75f)
            quadToRelative(-0.17f, 0f, -0.29f, -0.09f)
            reflectiveQuadTo(15.3f, 14.61f)
            lineTo(14.55f, 11.46f)
            quadToRelative(0.25f, 2.78f, -1.61f, 4.83f)
            reflectiveQuadTo(8.29f, 18.34f)
            close()
            moveToRelative(-0f, -1.4f)
            quadToRelative(2.05f, 0f, 3.49f, -1.44f)
            reflectiveQuadTo(13.23f, 12f)
            reflectiveQuadTo(11.79f, 8.5f)
            reflectiveQuadTo(8.29f, 7.06f)
            reflectiveQuadTo(4.8f, 8.5f)
            reflectiveQuadTo(3.35f, 12f)
            reflectiveQuadTo(4.8f, 15.5f)
            reflectiveQuadToRelative(3.49f, 1.44f)
            close()
            moveTo(8.29f, 12f)
            close()
          }
        }
        .build().also { _wbAutoW300 = it }
        IconWeight.W400 -> _wbAutoW400 ?: ImageVector.Builder(
          name = "wb_auto",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6.68f, 13.2f)
            horizontalLineToRelative(2.8f)
            lineTo(10f, 14.63f)
            quadToRelative(0.05f, 0.17f, 0.2f, 0.28f)
            reflectiveQuadTo(10.53f, 15f)
            quadToRelative(0.3f, 0f, 0.46f, -0.24f)
            quadToRelative(0.16f, -0.24f, 0.06f, -0.51f)
            lineTo(8.88f, 8.4f)
            quadTo(8.8f, 8.2f, 8.65f, 8.1f)
            reflectiveQuadTo(8.3f, 8f)
            horizontalLineTo(7.85f)
            quadTo(7.65f, 8f, 7.5f, 8.1f)
            reflectiveQuadTo(7.28f, 8.4f)
            lineTo(5.1f, 14.25f)
            quadTo(5f, 14.53f, 5.16f, 14.76f)
            reflectiveQuadTo(5.63f, 15f)
            quadToRelative(0.2f, 0f, 0.34f, -0.1f)
            reflectiveQuadTo(6.15f, 14.63f)
            lineTo(6.68f, 13.2f)
            close()
            moveToRelative(0.35f, -1f)
            lineToRelative(1f, -2.9f)
            horizontalLineToRelative(0.1f)
            lineToRelative(1f, 2.9f)
            horizontalLineTo(7.03f)
            close()
            moveTo(8.08f, 19f)
            quadTo(5.15f, 19f, 3.11f, 16.96f)
            quadTo(1.08f, 14.93f, 1.08f, 12f)
            quadToRelative(0f, -2.93f, 2.04f, -4.96f)
            reflectiveQuadTo(8.08f, 5f)
            quadToRelative(1.95f, 0f, 3.6f, 1f)
            reflectiveQuadToRelative(2.55f, 2.72f)
            horizontalLineTo(14.18f)
            quadTo(14.1f, 8.45f, 14.28f, 8.23f)
            reflectiveQuadTo(14.73f, 8f)
            quadToRelative(0.2f, 0f, 0.35f, 0.11f)
            quadToRelative(0.15f, 0.11f, 0.2f, 0.31f)
            lineToRelative(1.1f, 4.68f)
            horizontalLineToRelative(0.05f)
            lineTo(17.78f, 8.38f)
            quadTo(17.83f, 8.2f, 17.96f, 8.1f)
            reflectiveQuadTo(18.28f, 8f)
            horizontalLineToRelative(0.3f)
            quadToRelative(0.18f, 0f, 0.31f, 0.1f)
            quadToRelative(0.14f, 0.1f, 0.19f, 0.28f)
            lineToRelative(1.35f, 4.72f)
            horizontalLineToRelative(0.1f)
            lineToRelative(1.1f, -4.68f)
            quadToRelative(0.05f, -0.2f, 0.2f, -0.31f)
            reflectiveQuadTo(22.18f, 8f)
            quadToRelative(0.28f, 0f, 0.45f, 0.22f)
            reflectiveQuadToRelative(0.1f, 0.5f)
            lineTo(21.18f, 14.6f)
            quadToRelative(-0.05f, 0.2f, -0.19f, 0.3f)
            reflectiveQuadTo(20.65f, 15f)
            horizontalLineToRelative(-0.3f)
            quadTo(20.15f, 15f, 20f, 14.89f)
            quadTo(19.85f, 14.78f, 19.8f, 14.6f)
            lineTo(18.43f, 9.75f)
            horizontalLineTo(18.38f)
            lineTo(17.05f, 14.6f)
            quadTo(17f, 14.8f, 16.85f, 14.9f)
            reflectiveQuadTo(16.5f, 15f)
            horizontalLineTo(16.23f)
            quadToRelative(-0.2f, 0f, -0.35f, -0.11f)
            quadToRelative(-0.15f, -0.11f, -0.2f, -0.31f)
            lineTo(15.08f, 12.1f)
            quadToRelative(0f, 2.88f, -2.05f, 4.89f)
            reflectiveQuadTo(8.08f, 19f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(2.07f, 0f, 3.54f, -1.46f)
            reflectiveQuadTo(13.08f, 12f)
            quadToRelative(0f, -2.08f, -1.46f, -3.54f)
            reflectiveQuadTo(8.08f, 7f)
            reflectiveQuadTo(4.54f, 8.46f)
            reflectiveQuadTo(3.08f, 12f)
            reflectiveQuadToRelative(1.46f, 3.54f)
            reflectiveQuadTo(8.08f, 17f)
            close()
            moveToRelative(0f, -5f)
            close()
          }
        }
        .build().also { _wbAutoW400 = it }
        IconWeight.W500 -> _wbAutoW500 ?: ImageVector.Builder(
          name = "wb_auto",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(6.65f, 13.22f)
            horizontalLineTo(9.42f)
            lineToRelative(0.5f, 1.42f)
            quadToRelative(0.06f, 0.19f, 0.21f, 0.3f)
            reflectiveQuadToRelative(0.36f, 0.11f)
            quadToRelative(0.32f, 0f, 0.5f, -0.26f)
            reflectiveQuadToRelative(0.07f, -0.55f)
            lineTo(8.96f, 8.38f)
            quadTo(8.88f, 8.15f, 8.71f, 8.03f)
            reflectiveQuadTo(8.3f, 7.91f)
            horizontalLineTo(7.81f)
            quadTo(7.58f, 7.91f, 7.4f, 8.03f)
            reflectiveQuadTo(7.14f, 8.38f)
            lineTo(5.02f, 14.26f)
            quadTo(4.91f, 14.55f, 5.09f, 14.8f)
            reflectiveQuadToRelative(0.5f, 0.25f)
            quadToRelative(0.21f, 0f, 0.36f, -0.11f)
            reflectiveQuadToRelative(0.21f, -0.3f)
            lineToRelative(0.5f, -1.43f)
            close()
            moveTo(7f, 12.16f)
            lineTo(8f, 9.21f)
            horizontalLineTo(8.1f)
            lineToRelative(0.98f, 2.95f)
            horizontalLineTo(7f)
            close()
            moveToRelative(1.06f, 7.02f)
            quadToRelative(-3f, 0f, -5.09f, -2.09f)
            reflectiveQuadTo(0.88f, 12f)
            reflectiveQuadTo(2.97f, 6.91f)
            reflectiveQuadTo(8.06f, 4.81f)
            quadToRelative(1.99f, 0f, 3.68f, 1.04f)
            reflectiveQuadToRelative(2.58f, 2.84f)
            horizontalLineToRelative(-0.3f)
            quadTo(13.94f, 8.4f, 14.13f, 8.15f)
            reflectiveQuadTo(14.62f, 7.91f)
            quadToRelative(0.22f, 0f, 0.38f, 0.12f)
            reflectiveQuadToRelative(0.21f, 0.34f)
            lineToRelative(1.11f, 4.78f)
            horizontalLineToRelative(0.05f)
            lineTo(17.69f, 8.36f)
            quadTo(17.74f, 8.16f, 17.9f, 8.03f)
            reflectiveQuadTo(18.28f, 7.91f)
            horizontalLineToRelative(0.34f)
            quadToRelative(0.21f, 0f, 0.37f, 0.12f)
            reflectiveQuadToRelative(0.22f, 0.33f)
            lineToRelative(1.33f, 4.79f)
            horizontalLineToRelative(0.09f)
            lineTo(21.73f, 8.37f)
            quadTo(21.78f, 8.15f, 21.94f, 8.03f)
            reflectiveQuadTo(22.32f, 7.91f)
            quadToRelative(0.3f, 0f, 0.49f, 0.24f)
            reflectiveQuadToRelative(0.11f, 0.54f)
            lineToRelative(-1.54f, 5.9f)
            quadToRelative(-0.06f, 0.22f, -0.22f, 0.34f)
            reflectiveQuadToRelative(-0.39f, 0.12f)
            horizontalLineTo(20.43f)
            quadToRelative(-0.23f, 0f, -0.4f, -0.13f)
            reflectiveQuadTo(19.8f, 14.58f)
            lineTo(18.45f, 9.66f)
            horizontalLineTo(18.4f)
            lineToRelative(-1.31f, 4.92f)
            quadToRelative(-0.06f, 0.22f, -0.23f, 0.35f)
            reflectiveQuadToRelative(-0.4f, 0.12f)
            horizontalLineTo(16.14f)
            quadToRelative(-0.23f, 0f, -0.4f, -0.13f)
            reflectiveQuadTo(15.51f, 14.57f)
            lineToRelative(-0.46f, -1.9f)
            quadToRelative(-0.16f, 2.75f, -2.17f, 4.63f)
            reflectiveQuadTo(8.06f, 19.19f)
            close()
            moveTo(8.06f, 17f)
            quadToRelative(2.05f, 0f, 3.48f, -1.47f)
            reflectiveQuadTo(12.97f, 12f)
            reflectiveQuadTo(11.54f, 8.47f)
            reflectiveQuadTo(8.06f, 7f)
            quadTo(5.99f, 7f, 4.53f, 8.46f)
            reflectiveQuadTo(3.06f, 12f)
            reflectiveQuadToRelative(1.46f, 3.54f)
            reflectiveQuadTo(8.06f, 17f)
            close()
            moveTo(8.02f, 12f)
            close()
          }
        }
        .build().also { _wbAutoW500 = it }
    }

private var _wbAutoW300: ImageVector? = null
private var _wbAutoW400: ImageVector? = null
private var _wbAutoW500: ImageVector? = null

public val FujiIcons.FilterAltOff: ImageVector
    get() = when (FujiIconConfig.weight) {
        IconWeight.W300 -> _filterAltOffW300 ?: ImageVector.Builder(
          name = "filter_alt_off",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(14.32f, 11.62f)
            lineToRelative(-1f, -1f)
            lineTo(16.89f, 6.08f)
            horizontalLineTo(8.78f)
            lineTo(7.38f, 4.68f)
            horizontalLineTo(18.29f)
            quadToRelative(0.45f, 0f, 0.66f, 0.4f)
            reflectiveQuadTo(18.88f, 5.84f)
            lineToRelative(-4.56f, 5.78f)
            close()
            moveTo(13.4f, 15.38f)
            verticalLineToRelative(3.07f)
            quadToRelative(0f, 0.36f, -0.25f, 0.61f)
            reflectiveQuadToRelative(-0.61f, 0.25f)
            horizontalLineTo(11.47f)
            quadToRelative(-0.36f, 0f, -0.61f, -0.25f)
            reflectiveQuadTo(10.6f, 18.45f)
            verticalLineTo(12.58f)
            lineTo(2.79f, 4.77f)
            quadTo(2.59f, 4.57f, 2.58f, 4.28f)
            reflectiveQuadTo(2.79f, 3.78f)
            reflectiveQuadTo(3.29f, 3.56f)
            reflectiveQuadToRelative(0.5f, 0.22f)
            lineTo(20.22f, 20.2f)
            quadToRelative(0.21f, 0.21f, 0.21f, 0.49f)
            reflectiveQuadToRelative(-0.22f, 0.5f)
            quadToRelative(-0.22f, 0.21f, -0.5f, 0.22f)
            reflectiveQuadTo(19.22f, 21.2f)
            lineTo(13.4f, 15.38f)
            close()
            moveTo(13.31f, 10.62f)
            close()
          }
        }
        .build().also { _filterAltOffW300 = it }
        IconWeight.W400 -> _filterAltOffW400 ?: ImageVector.Builder(
          name = "filter_alt_off",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(14.8f, 11.98f)
            lineTo(13.38f, 10.55f)
            lineTo(16.95f, 6f)
            horizontalLineTo(8.83f)
            lineToRelative(-2f, -2f)
            horizontalLineTo(19f)
            quadToRelative(0.63f, 0f, 0.9f, 0.55f)
            reflectiveQuadTo(19.8f, 5.6f)
            lineToRelative(-5f, 6.38f)
            close()
            moveTo(14f, 16.83f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.43f, -0.29f, 0.71f)
            reflectiveQuadTo(13f, 20f)
            horizontalLineTo(11f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(10f, 19.43f, 10f, 19f)
            verticalLineTo(12.83f)
            lineTo(2.1f, 4.93f)
            quadTo(1.83f, 4.65f, 1.83f, 4.24f)
            reflectiveQuadTo(2.1f, 3.52f)
            quadTo(2.4f, 3.22f, 2.81f, 3.22f)
            quadToRelative(0.41f, 0f, 0.71f, 0.3f)
            lineTo(20.5f, 20.5f)
            quadToRelative(0.3f, 0.3f, 0.29f, 0.7f)
            reflectiveQuadToRelative(-0.31f, 0.7f)
            quadToRelative(-0.3f, 0.28f, -0.7f, 0.29f)
            reflectiveQuadTo(19.08f, 21.9f)
            lineTo(14f, 16.83f)
            close()
            moveTo(13.38f, 10.55f)
            close()
          }
        }
        .build().also { _filterAltOffW400 = it }
        IconWeight.W500 -> _filterAltOffW500 ?: ImageVector.Builder(
          name = "filter_alt_off",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(14.99f, 12.06f)
            lineTo(13.36f, 10.44f)
            lineTo(16.79f, 6.07f)
            horizontalLineTo(8.99f)
            lineTo(6.72f, 3.8f)
            horizontalLineTo(19f)
            quadToRelative(0.76f, 0f, 1.09f, 0.66f)
            reflectiveQuadTo(19.96f, 5.72f)
            lineToRelative(-4.97f, 6.35f)
            close()
            moveTo(14.2f, 17.06f)
            verticalLineTo(19f)
            quadToRelative(0f, 0.5f, -0.35f, 0.85f)
            reflectiveQuadTo(13f, 20.2f)
            horizontalLineTo(11f)
            quadToRelative(-0.5f, 0f, -0.85f, -0.35f)
            reflectiveQuadTo(9.8f, 19f)
            verticalLineTo(12.65f)
            lineTo(1.87f, 4.73f)
            quadTo(1.58f, 4.43f, 1.58f, 4f)
            reflectiveQuadTo(1.87f, 3.26f)
            quadTo(2.18f, 2.95f, 2.62f, 2.95f)
            reflectiveQuadTo(3.36f, 3.26f)
            lineToRelative(17.4f, 17.4f)
            quadToRelative(0.31f, 0.31f, 0.3f, 0.73f)
            reflectiveQuadToRelative(-0.32f, 0.73f)
            quadToRelative(-0.31f, 0.29f, -0.73f, 0.3f)
            reflectiveQuadToRelative(-0.73f, -0.3f)
            lineTo(14.2f, 17.06f)
            close()
            moveTo(13.36f, 10.44f)
            close()
          }
        }
        .build().also { _filterAltOffW500 = it }
    }

private var _filterAltOffW300: ImageVector? = null
private var _filterAltOffW400: ImageVector? = null
private var _filterAltOffW500: ImageVector? = null
