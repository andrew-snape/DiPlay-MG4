.class public final Lcom/allgo/carplay/service/ClusterNavBridge;
.super Ljava/lang/Object;
.source "ClusterNavBridge.java"


# static fields
.field private static final CONNECTION:Landroid/content/ServiceConnection;

.field private static final DESCRIPTOR:Ljava/lang/String; = "com.saicmotor.adapterservice.IMapService"

.field private static final LOCK:Ljava/lang/Object;

.field private static final MANEUVERS:Ljava/util/TreeMap;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/TreeMap<",
            "Ljava/lang/Integer;",
            "[I>;"
        }
    .end annotation
.end field

.field private static final PACKAGE:Ljava/lang/String; = "com.saicmotor.adapterservice"

.field private static final ROADS:Ljava/util/TreeMap;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/TreeMap<",
            "Ljava/lang/Integer;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field private static final SERVICE:Ljava/lang/String; = "com.saicmotor.adapterservice.services.MapService"

.field private static final TAG:Ljava/lang/String; = "RHD_CLUSTER"

.field private static final TX_GUIDE_INFOS:I = 0x1e

.field private static final TX_GUIDE_STATUS:I = 0x1d

.field private static final TX_IS_MAP_NAVIGATING:I = 0x16

.field private static final TX_REMAINING_DISTANCE:I = 0x27

.field private static final TX_REMAINING_TIMES:I = 0x26

.field private static final TX_ROAD_INFO:I = 0x1f

.field private static binder:Landroid/os/IBinder;

.field private static bound:Z

.field private static guiding:Z

.field private static rawDistance:I

.field private static remainMetres:I

.field private static remainMinutes:I

.field private static sentDistance:I

.field private static sentIcon:I

.field private static sentRemainMetres:I

.field private static sentRemainMinutes:I

.field private static sentRoad:Ljava/lang/String;

.field private static shownDistance:I


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .line 32
    new-instance v0, Ljava/lang/Object;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    sput-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->LOCK:Ljava/lang/Object;

    .line 38
    new-instance v0, Ljava/util/TreeMap;

    invoke-direct {v0}, Ljava/util/TreeMap;-><init>()V

    sput-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    .line 39
    new-instance v0, Ljava/util/TreeMap;

    invoke-direct {v0}, Ljava/util/TreeMap;-><init>()V

    sput-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->ROADS:Ljava/util/TreeMap;

    .line 40
    const/4 v0, -0x1

    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->rawDistance:I

    .line 41
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    .line 42
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMetres:I

    .line 43
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMinutes:I

    .line 46
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentIcon:I

    .line 47
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentDistance:I

    .line 49
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMetres:I

    .line 50
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMinutes:I

    .line 152
    new-instance v0, Lcom/allgo/carplay/service/ClusterNavBridge$1;

    invoke-direct {v0}, Lcom/allgo/carplay/service/ClusterNavBridge$1;-><init>()V

    sput-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->CONNECTION:Landroid/content/ServiceConnection;

    return-void
.end method

.method private constructor <init>()V
    .locals 0

    .line 52
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 53
    return-void
.end method

.method static synthetic access$000()Ljava/lang/Object;
    .locals 1

    .line 20
    sget-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->LOCK:Ljava/lang/Object;

    return-object v0
.end method

.method static synthetic access$102(Landroid/os/IBinder;)Landroid/os/IBinder;
    .locals 0

    .line 20
    sput-object p0, Lcom/allgo/carplay/service/ClusterNavBridge;->binder:Landroid/os/IBinder;

    return-object p0
.end method

.method static synthetic access$200()V
    .locals 0

    .line 20
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->publishLocked()V

    return-void
.end method

.method static synthetic access$302(Z)Z
    .locals 0

    .line 20
    sput-boolean p0, Lcom/allgo/carplay/service/ClusterNavBridge;->guiding:Z

    return p0
.end method

.method static synthetic access$400()V
    .locals 0

    .line 20
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->resetSentLocked()V

    return-void
.end method

.method private static call(Landroid/os/IBinder;IIILjava/lang/String;)Z
    .locals 4

    .line 234
    invoke-static {}, Landroid/os/Parcel;->obtain()Landroid/os/Parcel;

    move-result-object v0

    .line 235
    invoke-static {}, Landroid/os/Parcel;->obtain()Landroid/os/Parcel;

    move-result-object v1

    .line 237
    const/4 v2, 0x0

    :try_start_0
    const-string v3, "com.saicmotor.adapterservice.IMapService"

    invoke-virtual {v0, v3}, Landroid/os/Parcel;->writeInterfaceToken(Ljava/lang/String;)V

    .line 238
    const/16 v3, 0x1e

    if-ne p1, v3, :cond_0

    .line 239
    invoke-virtual {v0, p2}, Landroid/os/Parcel;->writeInt(I)V

    .line 240
    invoke-virtual {v0, p3}, Landroid/os/Parcel;->writeInt(I)V

    .line 241
    invoke-virtual {v0, p4}, Landroid/os/Parcel;->writeString(Ljava/lang/String;)V

    goto :goto_0

    .line 242
    :cond_0
    const/16 p3, 0x1f

    if-ne p1, p3, :cond_1

    .line 243
    invoke-virtual {v0, p4}, Landroid/os/Parcel;->writeString(Ljava/lang/String;)V

    goto :goto_0

    .line 245
    :cond_1
    invoke-virtual {v0, p2}, Landroid/os/Parcel;->writeInt(I)V

    .line 247
    :goto_0
    invoke-interface {p0, p1, v0, v1, v2}, Landroid/os/IBinder;->transact(ILandroid/os/Parcel;Landroid/os/Parcel;I)Z

    .line 248
    invoke-virtual {v1}, Landroid/os/Parcel;->readException()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 249
    nop

    .line 253
    invoke-virtual {v1}, Landroid/os/Parcel;->recycle()V

    .line 254
    invoke-virtual {v0}, Landroid/os/Parcel;->recycle()V

    .line 249
    const/4 p0, 0x1

    return p0

    .line 250
    :catchall_0
    move-exception p0

    .line 251
    nop

    .line 253
    invoke-virtual {v1}, Landroid/os/Parcel;->recycle()V

    .line 254
    invoke-virtual {v0}, Landroid/os/Parcel;->recycle()V

    .line 251
    return v2
.end method

.method private static clearLocked()V
    .locals 1

    .line 130
    sget-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-virtual {v0}, Ljava/util/TreeMap;->clear()V

    .line 131
    sget-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->ROADS:Ljava/util/TreeMap;

    invoke-virtual {v0}, Ljava/util/TreeMap;->clear()V

    .line 132
    const/4 v0, -0x1

    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->rawDistance:I

    .line 133
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    .line 134
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMetres:I

    .line 135
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMinutes:I

    .line 136
    return-void
.end method

.method private static ensureBoundLocked()V
    .locals 6

    .line 139
    const-string v0, "RHD_CLUSTER"

    sget-boolean v1, Lcom/allgo/carplay/service/ClusterNavBridge;->bound:Z

    if-eqz v1, :cond_0

    return-void

    .line 141
    :cond_0
    :try_start_0
    const-string v1, "android.app.ActivityThread"

    invoke-static {v1}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v1

    const-string v2, "currentApplication"

    const/4 v3, 0x0

    new-array v4, v3, [Ljava/lang/Class;

    .line 142
    invoke-virtual {v1, v2, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    new-array v2, v3, [Ljava/lang/Object;

    const/4 v3, 0x0

    invoke-virtual {v1, v3, v2}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/content/Context;

    .line 143
    if-nez v1, :cond_1

    return-void

    .line 144
    :cond_1
    new-instance v2, Landroid/content/Intent;

    invoke-direct {v2}, Landroid/content/Intent;-><init>()V

    new-instance v3, Landroid/content/ComponentName;

    const-string v4, "com.saicmotor.adapterservice"

    const-string v5, "com.saicmotor.adapterservice.services.MapService"

    invoke-direct {v3, v4, v5}, Landroid/content/ComponentName;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {v2, v3}, Landroid/content/Intent;->setComponent(Landroid/content/ComponentName;)Landroid/content/Intent;

    move-result-object v2

    .line 145
    sget-object v3, Lcom/allgo/carplay/service/ClusterNavBridge;->CONNECTION:Landroid/content/ServiceConnection;

    const/4 v4, 0x1

    invoke-virtual {v1, v2, v3, v4}, Landroid/content/Context;->bindService(Landroid/content/Intent;Landroid/content/ServiceConnection;I)Z

    move-result v1

    sput-boolean v1, Lcom/allgo/carplay/service/ClusterNavBridge;->bound:Z

    .line 146
    sget-boolean v1, Lcom/allgo/carplay/service/ClusterNavBridge;->bound:Z

    if-eqz v1, :cond_2

    const-string v1, "binding adapter MapService"

    goto :goto_0

    :cond_2
    const-string v1, "adapter MapService not available"

    :goto_0
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 149
    goto :goto_1

    .line 147
    :catchall_0
    move-exception v1

    .line 148
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "bind failed: "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 150
    :goto_1
    return-void
.end method

.method private static firstNumber(Ljava/lang/String;)D
    .locals 8

    .line 279
    nop

    .line 280
    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result v0

    const/4 v1, 0x0

    .line 281
    :goto_0
    if-ge v1, v0, :cond_0

    invoke-virtual {p0, v1}, Ljava/lang/String;->charAt(I)C

    move-result v2

    invoke-static {v2}, Ljava/lang/Character;->isDigit(C)Z

    move-result v2

    if-nez v2, :cond_0

    add-int/lit8 v1, v1, 0x1

    goto :goto_0

    .line 282
    :cond_0
    const-wide/high16 v2, -0x4010000000000000L    # -1.0

    if-lt v1, v0, :cond_1

    return-wide v2

    .line 283
    :cond_1
    move v4, v1

    .line 284
    :goto_1
    const/16 v5, 0x2e

    const/16 v6, 0x2c

    if-ge v4, v0, :cond_4

    invoke-virtual {p0, v4}, Ljava/lang/String;->charAt(I)C

    move-result v7

    invoke-static {v7}, Ljava/lang/Character;->isDigit(C)Z

    move-result v7

    if-nez v7, :cond_3

    .line 285
    invoke-virtual {p0, v4}, Ljava/lang/String;->charAt(I)C

    move-result v7

    if-eq v7, v5, :cond_2

    invoke-virtual {p0, v4}, Ljava/lang/String;->charAt(I)C

    move-result v7

    if-ne v7, v6, :cond_4

    :cond_2
    add-int/lit8 v7, v4, 0x1

    if-ge v7, v0, :cond_4

    invoke-virtual {p0, v7}, Ljava/lang/String;->charAt(I)C

    move-result v7

    invoke-static {v7}, Ljava/lang/Character;->isDigit(C)Z

    move-result v7

    if-eqz v7, :cond_4

    :cond_3
    add-int/lit8 v4, v4, 0x1

    goto :goto_1

    .line 287
    :cond_4
    :try_start_0
    invoke-virtual {p0, v1, v4}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p0, v6, v5}, Ljava/lang/String;->replace(CC)Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Ljava/lang/Double;->parseDouble(Ljava/lang/String;)D

    move-result-wide v0
    :try_end_0
    .catch Ljava/lang/NumberFormatException; {:try_start_0 .. :try_end_0} :catch_0

    return-wide v0

    .line 288
    :catch_0
    move-exception p0

    .line 289
    return-wide v2
.end method

.method static icon(II)I
    .locals 6

    .line 295
    const/4 v0, 0x0

    const/4 v1, 0x1

    if-ne p1, v1, :cond_0

    move p1, v1

    goto :goto_0

    :cond_0
    move p1, v0

    .line 296
    :goto_0
    const/16 v2, 0x1c

    const/16 v3, 0xc

    const/16 v4, 0x9

    if-lt p0, v2, :cond_3

    const/16 v2, 0x2e

    if-gt p0, v2, :cond_3

    .line 297
    add-int/lit8 p0, p0, -0x1b

    const/16 v0, 0xa

    const/16 v1, 0xb

    packed-switch p0, :pswitch_data_0

    .line 301
    return v3

    .line 300
    :pswitch_0
    if-eqz p1, :cond_1

    move v0, v1

    :cond_1
    return v0

    .line 299
    :pswitch_1
    return v4

    .line 298
    :pswitch_2
    if-eqz p1, :cond_2

    goto :goto_1

    :cond_2
    move v0, v1

    :goto_1
    return v0

    .line 304
    :cond_3
    const/16 v2, 0x25

    const/16 v5, 0x24

    packed-switch p0, :pswitch_data_1

    .line 324
    :pswitch_3
    return v0

    .line 308
    :pswitch_4
    const/4 p0, 0x4

    return p0

    .line 307
    :pswitch_5
    const/4 p0, 0x3

    return p0

    .line 310
    :pswitch_6
    const/4 p0, 0x7

    return p0

    .line 309
    :pswitch_7
    const/16 p0, 0x8

    return p0

    .line 323
    :pswitch_8
    const/16 p0, 0x16

    return p0

    .line 322
    :pswitch_9
    const/16 p0, 0x15

    return p0

    .line 317
    :pswitch_a
    return v2

    .line 316
    :pswitch_b
    return v5

    .line 312
    :pswitch_c
    return v3

    .line 320
    :pswitch_d
    const/16 p0, 0x13

    return p0

    .line 315
    :pswitch_e
    const/16 p0, 0x12

    return p0

    .line 314
    :pswitch_f
    const/16 p0, 0x11

    return p0

    .line 321
    :pswitch_10
    const/16 p0, 0x14

    return p0

    .line 319
    :pswitch_11
    if-eqz p1, :cond_4

    const/16 p0, 0x20

    goto :goto_2

    :cond_4
    const/16 p0, 0x21

    :goto_2
    return p0

    .line 318
    :pswitch_12
    if-eqz p1, :cond_5

    move v2, v5

    :cond_5
    return v2

    .line 313
    :pswitch_13
    return v4

    .line 311
    :pswitch_14
    if-eqz p1, :cond_6

    const/4 p0, 0x6

    goto :goto_3

    :cond_6
    const/4 p0, 0x5

    :goto_3
    return p0

    .line 306
    :pswitch_15
    const/4 p0, 0x2

    return p0

    .line 305
    :pswitch_16
    return v1

    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch

    :pswitch_data_1
    .packed-switch 0x1
        :pswitch_16
        :pswitch_15
        :pswitch_3
        :pswitch_14
        :pswitch_3
        :pswitch_13
        :pswitch_13
        :pswitch_12
        :pswitch_11
        :pswitch_10
        :pswitch_3
        :pswitch_10
        :pswitch_f
        :pswitch_e
        :pswitch_d
        :pswitch_d
        :pswitch_d
        :pswitch_14
        :pswitch_c
        :pswitch_16
        :pswitch_15
        :pswitch_b
        :pswitch_a
        :pswitch_9
        :pswitch_8
        :pswitch_14
        :pswitch_10
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_3
        :pswitch_7
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_f
        :pswitch_e
    .end packed-switch
.end method

.method public static onGuidance(Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;)V
    .locals 8

    .line 58
    :try_start_0
    sget-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->LOCK:Ljava/lang/Object;

    monitor-enter v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    .line 59
    if-nez p0, :cond_0

    :try_start_1
    monitor-exit v0

    return-void

    .line 60
    :cond_0
    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mValidInfo:I

    const/4 v2, 0x2

    and-int/2addr v1, v2

    if-eqz v1, :cond_2

    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mRouteState:I

    if-eqz v1, :cond_1

    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mRouteState:I

    if-ne v1, v2, :cond_2

    .line 61
    :cond_1
    const-string v1, "RHD_CLUSTER"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "route state "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    iget p0, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mRouteState:I

    invoke-virtual {v2, p0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p0

    const-string v2, ", guidance ends"

    invoke-virtual {p0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v1, p0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 62
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->clearLocked()V

    .line 63
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->publishLocked()V

    .line 64
    monitor-exit v0

    return-void

    .line 66
    :cond_2
    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mValidInfo:I

    and-int/lit16 v1, v1, 0x400

    const/4 v3, 0x1

    const/4 v4, 0x0

    if-eqz v1, :cond_6

    .line 67
    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mDistToNextManeuver:I

    invoke-static {v4, v1}, Ljava/lang/Math;->max(II)I

    move-result v1

    .line 69
    sget v5, Lcom/allgo/carplay/service/ClusterNavBridge;->rawDistance:I

    if-ltz v5, :cond_3

    sget v5, Lcom/allgo/carplay/service/ClusterNavBridge;->rawDistance:I

    add-int/lit8 v5, v5, 0x32

    if-le v1, v5, :cond_3

    sget-object v5, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-virtual {v5}, Ljava/util/TreeMap;->size()I

    move-result v5

    if-le v5, v3, :cond_3

    .line 70
    sget-object v5, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-virtual {v5}, Ljava/util/TreeMap;->firstKey()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/lang/Integer;

    invoke-virtual {v5}, Ljava/lang/Integer;->intValue()I

    move-result v5

    .line 71
    sget-object v6, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/util/TreeMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    .line 72
    sget-object v6, Lcom/allgo/carplay/service/ClusterNavBridge;->ROADS:Ljava/util/TreeMap;

    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v5

    invoke-virtual {v6, v5}, Ljava/util/TreeMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    .line 74
    :cond_3
    sput v1, Lcom/allgo/carplay/service/ClusterNavBridge;->rawDistance:I

    .line 75
    iget v5, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mValidInfo:I

    and-int/lit16 v5, v5, 0x800

    if-eqz v5, :cond_4

    iget-object v5, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mDistToNextManeuverDispStr:Ljava/lang/String;

    goto :goto_0

    :cond_4
    const/4 v5, 0x0

    .line 76
    :goto_0
    iget v6, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mValidInfo:I

    and-int/lit16 v6, v6, 0x1000

    if-eqz v6, :cond_5

    iget v6, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mDistToNextManeuverDispUnits:I

    goto :goto_1

    :cond_5
    const/4 v6, -0x1

    .line 77
    :goto_1
    invoke-static {v1, v5, v6}, Lcom/allgo/carplay/service/ClusterNavBridge;->shown(ILjava/lang/String;I)I

    move-result v1

    sput v1, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    .line 79
    :cond_6
    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mValidInfo:I

    and-int/lit16 v1, v1, 0x80

    if-eqz v1, :cond_7

    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mDistRemain:I

    invoke-static {v4, v1}, Ljava/lang/Math;->max(II)I

    move-result v1

    sput v1, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMetres:I

    .line 80
    :cond_7
    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mValidInfo:I

    and-int/lit8 v1, v1, 0x40

    if-eqz v1, :cond_8

    iget-object v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mTimeRemainToDest:[I

    if-eqz v1, :cond_8

    iget-object v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mTimeRemainToDest:[I

    array-length v1, v1

    if-lt v1, v2, :cond_8

    .line 82
    iget-object v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mTimeRemainToDest:[I

    aget v1, v1, v4

    mul-int/lit8 v1, v1, 0x3c

    iget-object p0, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteGuidanceUpdateInfo;->mTimeRemainToDest:[I

    aget p0, p0, v3

    add-int/2addr v1, p0

    invoke-static {v4, v1}, Ljava/lang/Math;->max(II)I

    move-result p0

    sput p0, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMinutes:I

    .line 84
    :cond_8
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->ensureBoundLocked()V

    .line 85
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->publishLocked()V

    .line 86
    monitor-exit v0

    .line 89
    goto :goto_2

    .line 86
    :catchall_0
    move-exception p0

    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    throw p0
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    .line 87
    :catchall_1
    move-exception p0

    .line 88
    const-string v0, "RHD_CLUSTER"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "onGuidance failed: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 90
    :goto_2
    return-void
.end method

.method public static onManeuver(Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;)V
    .locals 6

    .line 95
    :try_start_0
    sget-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->LOCK:Ljava/lang/Object;

    monitor-enter v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    .line 96
    if-eqz p0, :cond_5

    :try_start_1
    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mValidInfo:I

    and-int/lit8 v1, v1, 0x8

    if-nez v1, :cond_0

    goto/16 :goto_4

    .line 97
    :cond_0
    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mValidInfo:I

    and-int/lit8 v1, v1, 0x2

    if-eqz v1, :cond_1

    iget v1, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mIndex:I

    goto :goto_0

    :cond_1
    const/4 v1, 0x0

    .line 98
    :goto_0
    iget v2, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mValidInfo:I

    and-int/lit16 v2, v2, 0x100

    if-eqz v2, :cond_2

    iget v2, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mDriveSide:I

    goto :goto_1

    :cond_2
    const/4 v2, 0x1

    .line 99
    :goto_1
    sget-object v3, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v4

    iget v5, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mManeuverType:I

    filled-new-array {v5, v2}, [I

    move-result-object v2

    invoke-virtual {v3, v4, v2}, Ljava/util/TreeMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 100
    iget v2, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mValidInfo:I

    and-int/lit8 v2, v2, 0x10

    if-eqz v2, :cond_3

    iget-object v2, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mAfterManeuverRoadName:Ljava/lang/String;

    if-eqz v2, :cond_3

    .line 101
    iget-object p0, p0, Lcom/allgo/mirroring/control/NavRouteGuidanceUpdatesController$NavRouteManeuverUpdateInfo;->mAfterManeuverRoadName:Ljava/lang/String;

    invoke-virtual {p0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object p0

    goto :goto_2

    :cond_3
    const-string p0, ""

    .line 102
    :goto_2
    sget-object v2, Lcom/allgo/carplay/service/ClusterNavBridge;->ROADS:Ljava/util/TreeMap;

    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v1

    invoke-virtual {v2, v1, p0}, Ljava/util/TreeMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 104
    :goto_3
    sget-object p0, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-virtual {p0}, Ljava/util/TreeMap;->size()I

    move-result p0

    const/16 v1, 0x18

    if-le p0, v1, :cond_4

    .line 105
    sget-object p0, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-virtual {p0}, Ljava/util/TreeMap;->firstKey()Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ljava/lang/Integer;

    invoke-virtual {p0}, Ljava/lang/Integer;->intValue()I

    move-result p0

    .line 106
    sget-object v1, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/util/TreeMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    .line 107
    sget-object v1, Lcom/allgo/carplay/service/ClusterNavBridge;->ROADS:Ljava/util/TreeMap;

    invoke-static {p0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object p0

    invoke-virtual {v1, p0}, Ljava/util/TreeMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    .line 108
    goto :goto_3

    .line 109
    :cond_4
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->ensureBoundLocked()V

    .line 110
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->publishLocked()V

    .line 111
    monitor-exit v0

    .line 114
    goto :goto_5

    .line 96
    :cond_5
    :goto_4
    monitor-exit v0

    return-void

    .line 111
    :catchall_0
    move-exception p0

    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    throw p0
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    .line 112
    :catchall_1
    move-exception p0

    .line 113
    const-string v0, "RHD_CLUSTER"

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "onManeuver failed: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 115
    :goto_5
    return-void
.end method

.method private static publishLocked()V
    .locals 11

    .line 185
    sget-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->binder:Landroid/os/IBinder;

    .line 186
    if-eqz v0, :cond_e

    invoke-interface {v0}, Landroid/os/IBinder;->isBinderAlive()Z

    move-result v1

    if-nez v1, :cond_0

    goto/16 :goto_1

    .line 187
    :cond_0
    sget-object v1, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-virtual {v1}, Ljava/util/TreeMap;->isEmpty()Z

    move-result v1

    const/16 v2, 0x16

    const/16 v3, 0x1d

    const-string v4, "RHD_CLUSTER"

    const/4 v5, 0x0

    const/4 v6, 0x0

    if-nez v1, :cond_c

    sget v1, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    if-gez v1, :cond_1

    goto/16 :goto_0

    .line 197
    :cond_1
    sget-boolean v1, Lcom/allgo/carplay/service/ClusterNavBridge;->guiding:Z

    const/4 v7, 0x1

    if-nez v1, :cond_2

    .line 198
    invoke-static {v0, v2, v7, v6, v5}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    .line 199
    invoke-static {v0, v3, v7, v6, v5}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    move-result v1

    .line 200
    sput-boolean v7, Lcom/allgo/carplay/service/ClusterNavBridge;->guiding:Z

    .line 201
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "guidance started sent="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v4, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 203
    :cond_2
    sget-object v1, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-virtual {v1}, Ljava/util/TreeMap;->firstKey()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/Integer;

    invoke-virtual {v1}, Ljava/lang/Integer;->intValue()I

    move-result v1

    .line 204
    sget-object v2, Lcom/allgo/carplay/service/ClusterNavBridge;->MANEUVERS:Ljava/util/TreeMap;

    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/util/TreeMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, [I

    .line 205
    sget-object v3, Lcom/allgo/carplay/service/ClusterNavBridge;->ROADS:Ljava/util/TreeMap;

    invoke-static {v1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v1

    invoke-virtual {v3, v1}, Ljava/util/TreeMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/String;

    .line 206
    if-nez v1, :cond_3

    const-string v1, ""

    .line 207
    :cond_3
    aget v3, v2, v6

    aget v8, v2, v7

    invoke-static {v3, v8}, Lcom/allgo/carplay/service/ClusterNavBridge;->icon(II)I

    move-result v3

    .line 208
    sget v8, Lcom/allgo/carplay/service/ClusterNavBridge;->sentIcon:I

    if-ne v3, v8, :cond_4

    sget v8, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    sget v9, Lcom/allgo/carplay/service/ClusterNavBridge;->sentDistance:I

    if-ne v8, v9, :cond_4

    sget-object v8, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRoad:Ljava/lang/String;

    invoke-virtual {v1, v8}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v8

    if-nez v8, :cond_9

    .line 209
    :cond_4
    const/16 v8, 0x1e

    sget v9, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    invoke-static {v0, v8, v3, v9, v1}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    move-result v8

    .line 210
    sget-object v9, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRoad:Ljava/lang/String;

    invoke-virtual {v1, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v9

    if-nez v9, :cond_5

    const/16 v9, 0x1f

    invoke-static {v0, v9, v6, v6, v1}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    .line 211
    :cond_5
    sget v9, Lcom/allgo/carplay/service/ClusterNavBridge;->sentIcon:I

    if-ne v3, v9, :cond_6

    sget-object v9, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRoad:Ljava/lang/String;

    invoke-virtual {v1, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v9

    if-nez v9, :cond_7

    .line 212
    :cond_6
    new-instance v9, Ljava/lang/StringBuilder;

    invoke-direct {v9}, Ljava/lang/StringBuilder;-><init>()V

    const-string v10, "apple="

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v9

    aget v10, v2, v6

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v9

    const-string v10, " side="

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v9

    aget v2, v2, v7

    invoke-virtual {v9, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v7, " icon="

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v7, " distance="

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    sget v7, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v7, "m road=\""

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v7, "\" sent="

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v8}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v4, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 215
    :cond_7
    sget v2, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    sget v7, Lcom/allgo/carplay/service/ClusterNavBridge;->sentDistance:I

    if-eq v2, v7, :cond_8

    .line 216
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v7, "distance raw="

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    sget v7, Lcom/allgo/carplay/service/ClusterNavBridge;->rawDistance:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v7, "m shown="

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    sget v7, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v7, "m"

    invoke-virtual {v2, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v4, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 218
    :cond_8
    sput v3, Lcom/allgo/carplay/service/ClusterNavBridge;->sentIcon:I

    .line 219
    sget v2, Lcom/allgo/carplay/service/ClusterNavBridge;->shownDistance:I

    sput v2, Lcom/allgo/carplay/service/ClusterNavBridge;->sentDistance:I

    .line 220
    sput-object v1, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRoad:Ljava/lang/String;

    .line 222
    :cond_9
    sget v1, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMetres:I

    if-ltz v1, :cond_a

    sget v1, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMetres:I

    sget v2, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMetres:I

    if-eq v1, v2, :cond_a

    .line 223
    const/16 v1, 0x27

    sget v2, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMetres:I

    invoke-static {v0, v1, v2, v6, v5}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    .line 224
    sget v1, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMetres:I

    sput v1, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMetres:I

    .line 226
    :cond_a
    sget v1, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMinutes:I

    if-ltz v1, :cond_b

    sget v1, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMinutes:I

    sget v2, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMinutes:I

    if-eq v1, v2, :cond_b

    .line 227
    const/16 v1, 0x26

    sget v2, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMinutes:I

    invoke-static {v0, v1, v2, v6, v5}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    .line 228
    sget v0, Lcom/allgo/carplay/service/ClusterNavBridge;->remainMinutes:I

    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMinutes:I

    .line 230
    :cond_b
    return-void

    .line 188
    :cond_c
    :goto_0
    sget-boolean v1, Lcom/allgo/carplay/service/ClusterNavBridge;->guiding:Z

    if-eqz v1, :cond_d

    .line 189
    invoke-static {v0, v3, v6, v6, v5}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    .line 190
    invoke-static {v0, v2, v6, v6, v5}, Lcom/allgo/carplay/service/ClusterNavBridge;->call(Landroid/os/IBinder;IIILjava/lang/String;)Z

    .line 191
    sput-boolean v6, Lcom/allgo/carplay/service/ClusterNavBridge;->guiding:Z

    .line 192
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->resetSentLocked()V

    .line 193
    const-string v0, "guidance ended"

    invoke-static {v4, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 195
    :cond_d
    return-void

    .line 186
    :cond_e
    :goto_1
    return-void
.end method

.method private static resetSentLocked()V
    .locals 2

    .line 177
    const/4 v0, -0x1

    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentIcon:I

    .line 178
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentDistance:I

    .line 179
    const/4 v1, 0x0

    sput-object v1, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRoad:Ljava/lang/String;

    .line 180
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMetres:I

    .line 181
    sput v0, Lcom/allgo/carplay/service/ClusterNavBridge;->sentRemainMinutes:I

    .line 182
    return-void
.end method

.method static shown(ILjava/lang/String;I)I
    .locals 5

    .line 260
    const/16 v0, 0x64

    if-eqz p1, :cond_3

    if-ltz p2, :cond_3

    .line 261
    invoke-static {p1}, Lcom/allgo/carplay/service/ClusterNavBridge;->firstNumber(Ljava/lang/String;)D

    move-result-wide v1

    .line 262
    const-wide/16 v3, 0x0

    cmpl-double p1, v1, v3

    if-ltz p1, :cond_3

    .line 264
    if-nez p2, :cond_0

    const-wide p1, 0x408f400000000000L    # 1000.0

    mul-double/2addr v1, p1

    goto :goto_0

    .line 265
    :cond_0
    const/4 p1, 0x1

    if-eq p2, p1, :cond_2

    const/4 p1, 0x2

    if-ne p2, p1, :cond_1

    goto :goto_0

    .line 266
    :cond_1
    const-wide/high16 v1, -0x4010000000000000L    # -1.0

    .line 267
    :cond_2
    :goto_0
    cmpl-double p1, v1, v3

    if-ltz p1, :cond_3

    .line 268
    invoke-static {v1, v2}, Ljava/lang/Math;->round(D)J

    move-result-wide p1

    long-to-int p1, p1

    .line 269
    mul-int/lit8 p2, p0, 0x23

    div-int/2addr p2, v0

    const/16 v1, 0x96

    invoke-static {v1, p2}, Ljava/lang/Math;->max(II)I

    move-result p2

    .line 270
    sub-int v1, p1, p0

    invoke-static {v1}, Ljava/lang/Math;->abs(I)I

    move-result v1

    if-gt v1, p2, :cond_3

    return p1

    .line 274
    :cond_3
    const/16 p1, 0x1f4

    if-lt p0, p1, :cond_4

    goto :goto_1

    :cond_4
    if-lt p0, v0, :cond_5

    const/16 v0, 0x32

    goto :goto_1

    :cond_5
    const/16 v0, 0xa

    .line 275
    :goto_1
    div-int/lit8 p1, v0, 0x2

    add-int/2addr p0, p1

    div-int/2addr p0, v0

    mul-int/2addr p0, v0

    return p0
.end method

.method public static stop()V
    .locals 4

    .line 120
    :try_start_0
    sget-object v0, Lcom/allgo/carplay/service/ClusterNavBridge;->LOCK:Ljava/lang/Object;

    monitor-enter v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    .line 121
    :try_start_1
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->clearLocked()V

    .line 122
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->publishLocked()V

    .line 123
    monitor-exit v0

    .line 126
    goto :goto_0

    .line 123
    :catchall_0
    move-exception v1

    monitor-exit v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    throw v1
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    .line 124
    :catchall_1
    move-exception v0

    .line 125
    const-string v1, "RHD_CLUSTER"

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "stop failed: "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 127
    :goto_0
    return-void
.end method
