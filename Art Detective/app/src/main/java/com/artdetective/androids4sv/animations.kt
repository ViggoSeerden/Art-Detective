package com.artdetective.androids4sv

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.rotate

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun animateLazyColumn() {
    var list by remember {

        mutableStateOf(
            listOf("first", "second", "third")
        )
    }


    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(items = list, key = { it }) {
            Text(
                text = it,
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.animateItemPlacement(
                    tween(1000, easing = LinearEasing)
                )
            )
        }
    }

    Button(onClick = {
        list = list.shuffled() as MutableList<String>
    }) {
        Text(text = "Shuffle", color = Color.White)
    }
}

@Composable
fun rotationAnimation() {
    val infiniteTransition = rememberInfiniteTransition()

    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            RepeatMode.Reverse
        )
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {

        Image(
            imageVector = Icons.Default.Favorite,
            contentDescription = "",
            colorFilter = ColorFilter.tint(Color.Red),
            modifier = Modifier
                .size(350.dp)
                .rotate(angle)
        )
    }
}

@Composable
fun infiniteTransition() {
    val infiniteTransition = rememberInfiniteTransition()

    val color = infiniteTransition.animateColor(
        initialValue = Color.Red,
        targetValue = Color.Yellow,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(24.dp)
            .background(color.value)
    )
}

@Composable
fun slideInSlideOutAnimation() {
    Column(modifier = Modifier.fillMaxSize()) {
        var isVisible by remember { mutableStateOf(true) }

        Button(onClick = {
            isVisible = !isVisible
        }) {
            Text(text = if (isVisible) "Slide out" else "Slide in")
        }
        AnimatedVisibility(visible = isVisible, enter = slideIn(initialOffset = {
            IntOffset(it.width, it.height / 2)
        }, animationSpec = tween(2000, easing = LinearEasing)), exit = slideOut(targetOffset = {
            IntOffset(-it.width, it.height / 2)
        }, animationSpec = tween(2000, easing = LinearEasing))) {

            Box(
                modifier = Modifier
                    .background(Color.Yellow)
                    .size(400.dp)
            )
        }
    }
}

@Composable
fun drawPathUsingTouchEvent() {
    val ACTION_IDLE = 0
    val ACTION_DOWN = 1
    val ACTION_MOVE = 2
    val ACTION_UP = 3

    val path = remember { Path() }
    var motionEvent by remember { mutableStateOf(ACTION_IDLE) }
    var currentPosition by remember { mutableStateOf(Offset.Unspecified) }

    val drawModifier = Modifier
        .fillMaxWidth()
        .height(400.dp)
        .background(Color.Yellow)
        .clipToBounds()
        .pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {

                    // Wait for at least one pointer to press down, and set first contact position
                    val down: PointerInputChange = awaitFirstDown().also {
                        motionEvent = ACTION_DOWN
                        currentPosition = it.position
                    }


                    do {
                        // This PointerEvent contains details including events, id, position and more
                        val event: PointerEvent = awaitPointerEvent()

                        event.changes
                            .forEachIndexed { index: Int, pointerInputChange: PointerInputChange ->
                                // This necessary to prevent other gestures or scrolling
                                // when at least one pointer is down on canvas to draw
                                pointerInputChange.consumePositionChange()
                            }

                        motionEvent = ACTION_MOVE
                        currentPosition = event.changes.first().position
                    } while (event.changes.any { it.pressed })

                    motionEvent = ACTION_UP
                }
            }
        }




    Canvas(modifier = drawModifier) {

        when (motionEvent) {
            ACTION_DOWN -> {
                path.moveTo(currentPosition.x, currentPosition.y)
            }
            ACTION_MOVE -> {

                if (currentPosition != Offset.Unspecified) {
                    path.lineTo(currentPosition.x, currentPosition.y)
                }
            }

            ACTION_UP -> {
                path.lineTo(currentPosition.x, currentPosition.y)
                // Change state to idle to not draw in wrong position
                // if recomposition happens
                motionEvent = ACTION_IDLE
            }

            else -> Unit
        }

        drawPath(
            color = Color.Red,
            path = path,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
@Composable
fun drawPathAnimation() {
    val ACTION_IDLE = 0
    val ACTION_DOWN = 1
    val ACTION_MOVE = 2

    val path = remember { Path() }
    var eventState by remember { mutableStateOf(ACTION_IDLE) }

    var drawModifier =
        Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.Yellow)
            .clipToBounds()

    var targetIndexValue by remember {
        mutableStateOf(0)
    }

    val currentIndexValue by animateIntAsState(
        targetValue = targetIndexValue,
        animationSpec = tween(7000, easing = LinearEasing)
    )

    val points = parsePoint()
    val pointsCopy = mutableListOf<Offset>()

    LaunchedEffect(Unit) {
        targetIndexValue = points.size - 1
    }

    eventState = ACTION_DOWN

    Canvas(modifier = drawModifier) {
        pointsCopy.add(points.get(currentIndexValue))

        when (eventState) {
            ACTION_DOWN -> {
                path.moveTo(pointsCopy.get(0).x, pointsCopy.get(0).y)
                eventState = ACTION_MOVE
            }
            ACTION_MOVE -> {
                path.lineTo(
                    pointsCopy.get(currentIndexValue).x,
                    pointsCopy.get(currentIndexValue).y
                )
            }
            else -> Unit
        }

        drawPath(
            color = Color.Red,
            path = path,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}


@Composable
fun drawPointsAnimation(modifier : Modifier =  Modifier.fillMaxSize()) {
    var targetIndexValue by remember {
        mutableStateOf(0)
    }

    val currentIndex by animateIntAsState(
        targetValue = targetIndexValue,
        animationSpec = tween(7000, easing = LinearEasing)
    )

    Column(modifier = modifier ) {
        val points = parsePoint()
        val pointsCopy = mutableListOf<Offset>()

        LaunchedEffect(Unit) {
            targetIndexValue = points.size - 1
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            pointsCopy.add(points.get(currentIndex))

            drawPoints(
                points = pointsCopy,
                strokeWidth = 3f,
                pointMode = PointMode.Lines,
                color = Color.Yellow
            )
        }
    }
}


@Composable
fun parsePoint(): MutableList<Offset> {
    val jsonArray = JsonParser.parseString(
        "[[591.7699890136719,329.3479919433594],[586.7442474365234,320.7174530029297],[581.0932006835938,312.4783020019531],[575.1110534667969,304.4755401611328],[568.9042358398438,296.64544677734375],[562.5244293212891,288.9555892944336],[555.9980926513672,281.3896026611328],[549.3388671875,273.94029998779297],[542.5514678955078,266.6076354980469],[535.6327362060547,259.3987503051758],[528.5715942382812,252.32941436767578],[521.3451080322266,245.4292449951172],[513.9104309082031,238.75436401367188],[506.18145751953125,232.42388153076172],[497.9414367675781,226.7846450805664],[493.1193084716797,220.52841186523438],[495.62693786621094,210.85757064819336],[497.5863494873047,201.06097030639648],[498.9849548339844,191.16874313354492],[499.81565856933594,181.21273803710938],[500.07708740234375,171.22557067871094],[499.76356506347656,161.23997116088867],[498.8401641845703,151.2924461364746],[497.2659606933594,141.42719268798828],[495.00433349609375,131.69680786132812],[492.02488708496094,122.16195678710938],[488.3047332763672,112.89126968383789],[483.8296813964844,103.9607925415039],[478.5927734375,95.45506286621094],[472.5887908935547,87.47299766540527],[465.79283142089844,80.1552562713623],[458.2231140136719,77.83876609802246],[451.3427429199219,85.07632446289062],[445.2511901855469,92.99203681945801],[439.8952178955078,101.42359161376953],[435.2648468017578,110.27475547790527],[431.35618591308594,119.46773147583008],[428.16236877441406,128.9330596923828],[425.66966247558594,138.60693359375],[423.85755920410156,148.4312629699707],[422.69822692871094,158.35403442382812],[422.15879821777344,168.32991790771484],[422.2044372558594,178.32038116455078],[422.82337951660156,188.29175567626953],[424.01210021972656,198.2114143371582],[425.7634582519531,208.04734802246094],[428.0644989013672,217.76944732666016],[422.0479278564453,222.35591888427734],[415.71893310546875,220.1214828491211],[413.20338439941406,210.45261001586914],[410.1522521972656,200.9393081665039],[406.56842041015625,191.61357498168945],[402.459716796875,182.50691604614258],[397.8377685546875,173.64974975585938],[392.7187805175781,165.07015991210938],[387.1117477416992,156.80148315429688],[381.0055465698242,148.8945083618164],[374.3901062011719,141.4087791442871],[367.26318359375,134.4086570739746],[359.6320114135742,127.96231460571289],[351.5150375366211,122.14002990722656],[342.9413299560547,117.01453018188477],[333.95140075683594,112.66114044189453],[324.59356689453125,109.16953086853027],[314.9214096069336,106.68426895141602],[308.12357330322266,110.08691787719727],[306.2503356933594,119.89627838134766],[305.37957763671875,129.84659576416016],[305.39681243896484,139.8355369567871],[306.24603271484375,149.78858947753906],[307.8835906982422,159.64287948608398],[310.26636505126953,169.34421920776367],[313.3489685058594,178.8465461730957],[317.08255767822266,188.1127586364746],[321.41593170166016,197.1142349243164],[326.29752349853516,205.83086776733398],[331.67784881591797,214.24899291992188],[337.52989959716797,222.34635162353516],[343.83424377441406,230.0966567993164],[350.56890869140625,237.47603607177734],[357.7088088989258,244.46434783935547],[365.22689056396484,251.0439682006836],[373.09566497802734,257.20015716552734],[380.73006439208984,263.10107421875],[377.7938995361328,272.6471176147461],[371.8725891113281,272.5800552368164],[363.5384521484375,267.07027435302734],[354.9248733520508,262.00811767578125],[346.0599822998047,257.40040588378906],[336.9702606201172,253.2536849975586],[327.68223571777344,249.57262420654297],[318.22145080566406,246.36132049560547],[308.6130599975586,243.6227264404297],[298.88182830810547,241.35926818847656],[289.0521774291992,239.5712432861328],[279.1477737426758,238.25853729248047],[269.19210052490234,237.41961669921875],[259.20772552490234,237.05148315429688],[249.21717071533203,237.15015411376953],[239.2420883178711,237.7160186767578],[229.30513763427734,238.75337982177734],[219.42913818359375,240.2649383544922],[209.63748168945312,242.25112915039062],[199.9546012878418,244.71309661865234],[190.40468978881836,247.64868927001953],[181.01291275024414,251.0563735961914],[171.80475616455078,254.93301391601562],[162.8061294555664,259.27396392822266],[154.04390335083008,264.07386779785156],[145.54542922973633,269.32677459716797],[137.339111328125,275.0249710083008],[129.4535369873047,281.15950775146484],[121.91910171508789,287.72054290771484],[114.76655006408691,294.69569396972656],[108.0279483795166,302.07152557373047],[101.73512268066406,309.8310089111328],[95.92151832580566,317.95567321777344],[90.61984252929688,326.4232711791992],[85.86211013793945,335.2079391479492],[79.2756290435791,337.34285736083984],[69.50605392456055,335.618896484375],[59.913350105285645,338.1150360107422],[52.15889930725098,344.3108444213867],[47.32365608215332,352.98523712158203],[46.15491962432861,362.84178161621094],[48.899155616760254,372.38336181640625],[55.02856254577637,380.19683837890625],[63.69694995880127,385.0200424194336],[69.83750915527344,389.09102630615234],[67.89340114593506,398.884033203125],[66.96266460418701,408.8321990966797],[66.26050090789795,418.7993621826172],[65.7549295425415,428.77838134765625],[65.44372177124023,438.76531982421875],[65.33650779724121,448.7565612792969],[65.44646549224854,458.74766540527344],[65.79753017425537,468.7333221435547],[66.41775703430176,478.7056579589844],[67.34498023986816,488.6540069580078],[68.62902450561523,498.56248474121094],[70.33610534667969,508.40655517578125],[72.55578994750977,518.1475830078125],[75.41056442260742,527.7206115722656],[79.07015609741211,537.0139923095703],[83.76596450805664,545.825439453125],[89.7840805053711,553.7838439941406],[97.35901641845703,560.2608795166016],[106.38390159606934,564.4679107666016],[116.22900772094727,565.9886016845703],[126.22101593017578,566],[136.2129898071289,566],[146.2050552368164,566],[156.19702911376953,566],[166.18900299072266,566],[176.18106842041016,566],[186.1729507446289,566],[196.1650161743164,566],[206.15689849853516,566],[216.14896392822266,566],[226.14102935791016,566],[236.1329116821289,566],[246.1249771118164,566],[256.11685943603516,566],[266.10892486572266,566],[276.10099029541016,566],[286.0928726196289,566],[296.0849380493164,566],[301.4155960083008,561.7408294677734],[297.8159713745117,552.4902496337891],[291.1748733520508,545.0780487060547],[282.8695297241211,539.5537567138672],[273.7573776245117,535.4721069335938],[264.23687744140625,532.4519653320312],[254.49463653564453,530.2419281005859],[244.62632751464844,528.6846618652344],[234.68582916259766,527.6844024658203],[224.70699310302734,527.1898803710938],[220.18106079101562,526.0540771484375],[229.2045440673828,521.7688140869141],[237.85437774658203,516.7720947265625],[246.0748062133789,511.09669494628906],[253.81371307373047,504.7804718017578],[261.0210876464844,497.8639373779297],[267.6495056152344,490.390625],[273.65465545654297,482.407958984375],[278.99478912353516,473.96592712402344],[283.63170623779297,465.11822509765625],[287.53228759765625,455.9220733642578],[290.66844940185547,446.4379425048828],[293.0189514160156,436.72900390625],[294.5714797973633,426.8614044189453],[295.3220748901367,416.90028381347656],[295.2767791748047,406.9110565185547],[294.43467712402344,396.9573059082031],[292.7920608520508,387.10394287109375],[290.3541030883789,377.4168167114258],[287.1347427368164,367.96063232421875],[283.15563201904297,358.7982406616211],[278.4457702636719,349.9890823364258],[273.0375213623047,341.5902633666992],[266.9698181152344,333.65515899658203],[260.2833786010742,326.23384857177734],[253.02194213867188,319.3740768432617],[245.2329559326172,313.1195602416992],[236.96588134765625,307.51220703125],[228.27359008789062,302.5903091430664],[219.21128845214844,298.38787841796875],[209.83802795410156,294.93444061279297],[200.21526336669922,292.25343322753906],[190.40687561035156,290.36124420166016],[181.85412216186523,289.26480865478516],[191.83935546875,288.9732360839844],[201.81648635864258,289.46114349365234],[211.72252655029297,290.7485580444336],[221.49191284179688,292.83385467529297],[231.05896759033203,295.7063446044922],[240.36095428466797,299.3478317260742],[249.33657836914062,303.7321090698242],[257.9280700683594,308.8283004760742],[266.0809097290039,314.6006088256836],[273.74332427978516,321.0093460083008],[280.86692810058594,328.01219177246094],[287.40599060058594,335.56375885009766],[293.3174819946289,343.6162338256836],[298.5613250732422,352.1184616088867],[303.1008529663086,361.01649475097656],[306.9040222167969,370.2533874511719],[309.94398498535156,379.7688293457031],[312.2004852294922,389.4998550415039],[313.66104888916016,399.3817825317383],[314.3223342895508,409.34906005859375],[314.2061309814453,419.3382873535156],[313.3496780395508,429.2908935546875],[311.71871185302734,439.1460876464844],[309.28375244140625,448.8340606689453],[306.02703857421875,458.2769012451172],[301.9468078613281,467.3940887451172],[297.05823516845703,476.1043701171875],[291.39439392089844,484.3319396972656],[285.00440216064453,492.00909423828125],[277.94969177246094,499.0803527832031],[270.3016052246094,505.5056610107422],[262.13512420654297,511.25823974609375],[253.52660369873047,516.3255462646484],[244.54901123046875,520.70703125],[244.99798583984375,522.6557312011719],[254.97618103027344,523.1535491943359],[264.90484619140625,524.2608795166016],[274.73728942871094,526.0275268554688],[284.4027633666992,528.5489349365234],[293.78245544433594,531.9786376953125],[302.6614685058594,536.5431365966797],[310.6337432861328,542.5389709472656],[316.98038482666016,550.2164459228516],[322.5601119995117,556.0634613037109],[331.94087982177734,552.6261138916016],[341.1307830810547,548.7065124511719],[350.1040725708008,544.3131256103516],[358.83538818359375,539.4574432373047],[367.29991912841797,534.1500396728516],[375.4713668823242,528.4019470214844],[383.32421112060547,522.2254333496094],[390.8313980102539,515.6330413818359],[397.96484375,508.63832092285156],[404.6946105957031,504.9378967285156],[411.2626953125,512.4676513671875],[417.83091735839844,519.9975891113281],[424.3991394042969,527.5275268554688],[430.96726989746094,535.0573272705078],[437.5354919433594,542.5872650146484],[444.10357666015625,550.1170654296875],[450.6717987060547,557.6469573974609],[457.2400207519531,565.1768951416016],[466.8576354980469,566],[476.8497009277344,566],[486.8415832519531,566],[496.8336486816406,566],[506.8257141113281,566],[516.8175964355469,566],[526.8096618652344,566],[530.3993988037109,558.8936767578125],[528.8008422851562,549.0879211425781],[523.4002838134766,540.7749328613281],[515.2272796630859,535.1046142578125],[505.83799743652344,531.7388610839844],[496.00852966308594,529.9884185791016],[486.037841796875,529.4109954833984],[478.10479736328125,527.1965179443359],[476.2844543457031,517.3718566894531],[474.4640655517578,507.54701232910156],[472.6437225341797,497.72235107421875],[478.874755859375,491.1739501953125],[486.4937744140625,484.7159118652344],[493.4033966064453,477.5046691894531],[499.4949493408203,469.590576171875],[504.6870880126953,461.05950927734375],[508.9325714111328,452.0193634033203],[512.2222442626953,442.5890197753906],[514.5783538818359,432.8826446533203],[516.0411834716797,423.0015869140625],[516.6562805175781,413.0315856933594],[516.4580230712891,403.0442123413086],[515.4397277832031,393.10728454589844],[518.6825256347656,387.38734436035156],[528.6263427734375,386.44810485839844],[538.4341125488281,384.55811309814453],[548.0250854492188,381.76831817626953],[557.317138671875,378.10433197021484],[566.2127227783203,373.56251525878906],[574.5723571777344,368.100341796875],[582.1690368652344,361.62223052978516],[588.5776824951172,353.9773483276367],[592.9320678710938,345.0261688232422],[593.6643524169922,335.1475372314453],[517.1840362548828,321.25516510009766],[510.12632751464844,314.6090087890625],[510.46278381347656,304.92002868652344],[517.9642944335938,298.7801818847656],[527.52587890625,300.3759002685547],[532.6324157714844,308.61427307128906],[529.8119964599609,317.88790130615234]]"
    ).asJsonArray
    val points = remember {
        mutableStateListOf<Offset>()
    }

    for (i in 0 until jsonArray.size()) {
        var offset = jsonArray.get(i).asJsonArray
        points.add(Offset(offset.get(0).asFloat, offset.get(1).asFloat))
    }

    return points
}

@Composable
fun FadeInFadeOutHeart() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        var isVisible by remember {
            mutableStateOf(false)
        }

        Button(onClick = {
            isVisible = !isVisible
        }) {
            Text(text = if (isVisible) "Hide" else "Show")
        }

        Spacer(modifier = Modifier.size(150.dp))


        AnimatedVisibility(
            isVisible,
            exit = fadeOut(animationSpec = tween(2000, easing = LinearEasing)),
            enter = fadeIn(animationSpec = tween(2000, easing = LinearEasing))
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(Color.Red, shape = heart())
            )
        }

    }
}

@Composable
fun AnimatedContentSize() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Color.Gray
                )
                .animateContentSize()
        ) {

            var isExpended by remember {
                mutableStateOf(false)
            }

            Row(modifier = Modifier
                .padding(16.dp)
                .clickable {
                    isExpended = !isExpended
                }) {

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "",
                    tint = Color.White
                )
                Text(
                    text = if (isExpended) "Hide more info" else "Click for more information...",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp, 0.dp, 0.dp, 0.dp)
                )


            }

            if (isExpended)
                Text(text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            isExpended = !isExpended

                        })

        }


    }
}
