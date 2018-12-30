package com.ilyak.pifarm.migrations

import slick.jdbc.H2Profile

import scala.concurrent.ExecutionContext

class Migration(db: H2Profile)(action: H2Profile => Unit)
               (implicit ec: ExecutionContext) {

}
