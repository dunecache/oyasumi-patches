package app.adm.patches.media

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance

object WebPageStartedFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/Web" + '$' + "i;",
    name = "onPageStarted",
    returnType = "V",
    parameters = listOf(
        "Landroid/webkit/WebView;",
        "Ljava/lang/String;",
        "Landroid/graphics/Bitmap;"
    ),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/dv/get/Web;",
            name = "W1",
            parameters = listOf("Lcom/dv/get/Web;"),
            returnType = "Landroid/webkit/WebView;"
        )
    )
)

object WebRequestFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/Web" + '$' + "i;",
    name = "shouldInterceptRequest",
    returnType = "Landroid/webkit/WebResourceResponse;",
    parameters = listOf(
        "Landroid/webkit/WebView;",
        "Ljava/lang/String;"
    ),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/dv/get/Web;",
            name = "G2",
            parameters = listOf("Lcom/dv/get/Web;"),
            returnType = "Lcom/dv/get/all/MyActivity;"
        ),
        newInstance("Landroid/webkit/WebResourceResponse;"),
        methodCall(
            definingClass = "Landroid/webkit/WebViewClient;",
            name = "shouldInterceptRequest",
            parameters = listOf(
                "Landroid/webkit/WebView;",
                "Ljava/lang/String;"
            ),
            returnType = "Landroid/webkit/WebResourceResponse;"
        )
    )
)

object WebCreateOptionsMenuFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/Web;",
    name = "onCreateOptionsMenu",
    returnType = "Z",
    parameters = listOf("Landroid/view/Menu;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/Menu;",
            name = "clear"
        ),
        methodCall(
            definingClass = "Landroid/view/Menu;",
            name = "add",
            parameters = listOf("I", "I", "I", "I"),
            returnType = "Landroid/view/MenuItem;"
        ),
        methodCall(
            definingClass = "Landroid/view/MenuItem;",
            name = "setShowAsAction",
            parameters = listOf("I"),
            returnType = "Landroid/view/MenuItem;"
        )
    )
)

object WebOptionsItemSelectedFingerprint : Fingerprint(
    definingClass = "Lcom/dv/get/Web;",
    name = "onOptionsItemSelected",
    returnType = "Z",
    parameters = listOf("Landroid/view/MenuItem;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/view/MenuItem;",
            name = "getItemId"
        )
    )
)
