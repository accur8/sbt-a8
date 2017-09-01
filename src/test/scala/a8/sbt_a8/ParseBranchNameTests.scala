package a8.sbt_a8

import org.scalatest.FunSuite


class ParseBranchNameTests extends FunSuite {

  val testData = List(
    " (HEAD -> master, origin/master)" -> "master",
    " (HEAD -> master)" -> "master",
    " (HEAD -> user/feature-branch, origin/user/feature-branch)" -> "userfeaturebranch",
    " (HEAD -> user/feature-branch)" -> "userfeaturebranch",
    " (HEAD -> user/feature-branch-1, origin/user/feature-branch-2)" -> "userfeaturebranch1",
    " (HEAD -> user/feature-branch/sub, origin/user/feature-branch/sub)" -> "userfeaturebranchsub",
    " (HEAD -> user/feature-branch/sub)" -> "userfeaturebranchsub",
    " (HEAD -> master, origin/master, origin/HEAD, user/feature-branch)" -> "master",
    " (HEAD -> master, origin/master, origin/HEAD)" -> "master"
  )

  testData.foreach { item =>
    test(item._1) {
      val result = parseGitBranchName(item._1)
      assert(result == item._2)
    }
  }

}
