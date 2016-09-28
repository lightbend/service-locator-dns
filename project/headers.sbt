// Copyright © 2016 Lightbend, Inc. All rights reserved.
// No information contained herein may be reproduced or transmitted in any form
// or by any means without the express written permission of Typesafe, Inc.

// This is to add copyright headers to the build files

import de.heikoseeberger.sbtheader.HeaderPattern

headers := Map(
  "scala" -> (
    HeaderPattern.cStyleBlockComment,
    """|/*
       | * Copyright © 2016 Lightbend, Inc. All rights reserved.
       | * No information contained herein may be reproduced or transmitted in any form
       | * or by any means without the express written permission of Lightbend, Inc.
       | */
       |
       |""".stripMargin
    )
)
