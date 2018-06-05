package osmesa.server

import geotrellis.vector.Extent
import geotrellis.vectortile.VectorTile

package object tile {

  lazy val emptyVectorTile = {
    val extent = Extent(0, 0, 1, 1)
    VectorTile(Map(), extent)
  }

}
