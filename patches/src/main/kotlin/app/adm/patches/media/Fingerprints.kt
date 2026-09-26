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

object PopupShowFingerprint : Fingerprint(
    definingClass = "Lb2/f;",
    name = "e",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "Landroidx/appcompat/widget/d0;",
            name = "e",
            parameters = listOf("Landroid/widget/PopupMenu;"),
            returnType = "V"
        ),
        methodCall(
            definingClass = "Landroid/widget/PopupMenu;",
            name = "show",
            returnType = "V"
        )
    )
)

object PopupItemClickFingerprint : Fingerprint(
    definingClass = "Lb2/e;",
    name = "onMenuItemClick",
    returnType = "Z",
    parameters = listOf("Landroid/view/MenuItem;"),
    filters = listOf(
        methodCall(
            definingClass = "Lb2/f$b;",
            name = "b",
            parameters = listOf("Landroid/view/MenuItem;"),
            returnType = "V"
        )
    )
)
