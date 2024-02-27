package com.resonance.hawk.proxy

import com.resonance.hawk.util.SEPARATOR
import com.resonance.hawk.util.write
import org.spongycastle.asn1.DERSequence
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x500.X500NameBuilder
import org.spongycastle.asn1.x500.style.BCStyle
import org.spongycastle.asn1.x509.Extension
import org.spongycastle.asn1.x509.GeneralName
import org.spongycastle.asn1.x509.GeneralNames
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.operator.ContentSigner
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.net.Socket
import java.security.*
import java.util.*
import javax.net.ssl.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class UniSocket {
    private val keyPair: KeyPair
    private val signer: ContentSigner
    private val random = SecureRandom()
    private val subjectPublicKeyInfo: SubjectPublicKeyInfo
    private val certificates = HashMap<String, SSLSocketFactory>()

    init {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        keyPair = keyPairGenerator.genKeyPair()
        signer =
            JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BouncyCastleProvider())
                .build(keyPair.private)

        subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
    }

    suspend fun create(socket: Socket): Pair<Socket, String?> {
        val inputStream = socket.getInputStream()
        return if (inputStream.read().toChar() == 'C') {
            // Https CONNECT Request
            val leadRequest = Request.fromInputStream(inputStream, null)
            val socketFactory = getSSLFactory(leadRequest.host)
            val response = try {
                Socket(leadRequest.host, leadRequest.port)
                "HTTP/1.1 200 Connection Established$SEPARATOR$SEPARATOR".toByteArray()
            } catch (e: Exception) {
                "HTTP/1.1 403 Forbidden$SEPARATOR$SEPARATOR".toByteArray()
            }
            socket.write(response)
            val sslSocket = socketFactory.createSocket(
                socket,
                socket.inetAddress.hostAddress,
                socket.port,
                false
            ) as SSLSocket
            sslSocket.useClientMode = false
            sslSocket.startHandshake()
            Pair(sslSocket, "${leadRequest.host}:${leadRequest.port}")
        } else
            Pair(socket, null)
    }

    private fun getSSLFactory(domain: String): SSLSocketFactory {
        if (certificates[domain] != null)
            return certificates[domain]!!
        val startDate = Date(System.currentTimeMillis() - 86400000) // 24 * 60 * 60 * 1000
        val endDate = Date(System.currentTimeMillis() + 864000000)
        val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.O, "Resonance")
            .addRDN(BCStyle.OU, "0x000f")
            .addRDN(BCStyle.L, "Kerala")
            .addRDN(BCStyle.CN, domain)
        val x500Name: X500Name = nameBuilder.build()
        val certGen = X509v3CertificateBuilder(
            x500Name, BigInteger.valueOf(random.nextLong()),
            startDate, endDate, x500Name, subjectPublicKeyInfo
        )
        val altName = ArrayList<GeneralName>()
        altName.add(GeneralName(GeneralName.dNSName, domain))
        val subjectAltNames =
            GeneralNames.getInstance(DERSequence(altName.toArray(arrayOf<GeneralName>()) as Array<GeneralName?>))
        certGen.addExtension(Extension.subjectAlternativeName, false, subjectAltNames)
        val certificate =
            JcaX509CertificateConverter().setProvider(BouncyCastleProvider()).getCertificate(
                certGen.build(
                    signer
                )
            )
        val keyStore = KeyStore.getInstance("BKS")
        keyStore.load(null, null)
        keyStore.setKeyEntry(
            "0x000f", keyPair.private, "Ioipwer1@".toCharArray(), arrayOf(
                certificate
            )
        )
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "Ioipwer1@".toCharArray())
        val sslContext = SSLContext.getInstance("TLSv1.2")
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        sslContext.init(
            keyManagerFactory.keyManagers,
            trustManagerFactory.trustManagers,
            SecureRandom()
        )
        certificates[domain] = sslContext.socketFactory
        return sslContext.socketFactory ?: throw Exception("Unable to create certificate")
    }
}
