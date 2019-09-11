package osmesa.server.tile

import geotrellis.proj4.WebMercator
import geotrellis.spark.tiling._

object TileLayouts {
  private val layouts: Array[LayoutDefinition] = (0 to 30)
    .map({ n =>
      ZoomedLayoutScheme.layoutForZoom(n, WebMercator.worldExtent, 256)
    })
    .toArray

  def apply(i: Int) = layouts(i)
}
