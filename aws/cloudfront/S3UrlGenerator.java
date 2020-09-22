package com.samsung.msmw.biz.common.util;

import java.io.File;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.samsung.msmw.biz.common.model.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.samsung.root.core.utils.StringUtils;

@Component
public class S3UrlGenerator {

    @Value("${config.s3.uri}")
    private String s3Uri;

    @Value("${config.cloudfront.distributionDomainName}")
    private String distributionDomainName;

    @Value("${config.cloudfront.PrivateKeyFile}")
    private String cloudFrontPrivateKeyFileName;

    @Value("${config.cloudfront.keyPairId}")
    private String cloudFrontKeyPairId;

    private final int cloudFrontExpiredTime = 60 * 60 * 1000;   // ScHong, 170621, TPC-1003, change from 1 min to 1 hour

    // For onetimeUrl
    private File cloudFrontPrivateKeyFile;
    private String awsBucketName;

    private AWSSecurityTokenServiceClient awsStsClient;

    final private Logger logger = LoggerFactory.getLogger(S3UrlGenerator.class);

    public S3UrlGenerator() {
    }

    public S3UrlGenerator(String awsBucketName, AWSSecurityTokenServiceClient awsStsClient) {
        this.awsBucketName = awsBucketName;
        this.awsStsClient = awsStsClient;
    }

    /**
     * 
     * @param path
     *            ex)
     *            1_infra_test/1/lmsadmin_1/course_icon/164/1381379539184_1.jpg
     * @return without s3 token
     */

    public String getS3UrlWithOutTokenForPrefixPath(String path) {
        return s3Uri + "/" + path;
    }

    /**
     * Make onetimeUrl of image in S3
     * 
     * @param targetPath
     * @return
     */

    public String getSignedURLWithCannedPolicy(String targetPath) {
        String signedUrl = "";
        if (StringUtils.isNotEmpty(targetPath)) {
            Date expirationDate = new Date(System.currentTimeMillis() + cloudFrontExpiredTime);

            if (cloudFrontPrivateKeyFile == null) {
                String rpathSet = Constants.SERVLET_WEB_INF_REAL_PATH;
                if (rpathSet != null) {
                    StringBuffer cloudFrontKeyfile = new StringBuffer(rpathSet).append(File.separator).append("cloudfront").append(File.separator).append(cloudFrontPrivateKeyFileName);
                    cloudFrontPrivateKeyFile = new File(cloudFrontKeyfile.toString());
                }
            }

            if (cloudFrontPrivateKeyFile != null) {
                try {
                    signedUrl = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                        Protocol.https,
                        distributionDomainName,
                        cloudFrontPrivateKeyFile,
                        targetPath, // the resource path to our content
                        cloudFrontKeyPairId,
                        expirationDate);
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // logger.info("targetPath : " + targetPath);
                logger.info("signedUrl : " + signedUrl);
            }

        }

        return signedUrl;
    }
}
