# Security and privacy

The reference app processes local images, videos, and audio. Face enrollment,
input media, and generated reports can contain personal information. Do not
attach private media, face embeddings, recordings, transcripts, signing keys,
tokens, or unredacted device dumps to public issues.

Use GitHub private vulnerability reporting when it is available for this
repository. Otherwise open a minimal issue requesting a private reporting
channel, without exploit details or sensitive data.

The release APK is a development reference build. Treat the models and sample
observations as examples; validate your own deployment, consent, access controls,
retention policy, and device behavior before using them with real users.

The asset downloader uses an explicit file allowlist, bounded ZIP extraction,
SHA-256 verification, and verified replacement. Keep its tests passing when
changing the asset format or download paths.
