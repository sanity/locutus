package locutus.tools.crypto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import locutus.tools.ByteArraySegment

class UtilSpec : FunSpec({
    test("ByteArray merge") {
        val arrays = listOf(byteArrayOf(1, 2), byteArrayOf(5, 8), byteArrayOf()).map { ByteArraySegment(it) }
        arrays.merge() shouldBe byteArrayOf(1, 2, 5, 8)
    }
})

/*
Runtime JAR files in the classpath should have the same version. These files were found in the classpath:
    /home/ian/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk8/1.3.72/916d54b9eb6442b615e6f1488978f551c0674720/kotlin-stdlib-jdk8-1.3.72.jar (version 1.3)
    /home/ian/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-jdk7/1.3.72/3adfc2f4ea4243e01204be8081fe63bde6b12815/kotlin-stdlib-jdk7-1.3.72.jar (version 1.3)
    /home/ian/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.4.0/63e75298e93d4ae0b299bb869cf0c627196f8843/kotlin-stdlib-1.4.0.jar (version 1.4)
    /home/ian/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib-common/1.4.0/1c752cce0ead8d504ccc88a4fed6471fd83ab0dd/kotlin-stdlib-common-1.4.0.jar (version 1.4)
 */