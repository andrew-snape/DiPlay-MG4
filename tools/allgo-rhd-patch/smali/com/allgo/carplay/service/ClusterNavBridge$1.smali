.class Lcom/allgo/carplay/service/ClusterNavBridge$1;
.super Ljava/lang/Object;
.source "ClusterNavBridge.java"

# interfaces
.implements Landroid/content/ServiceConnection;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/allgo/carplay/service/ClusterNavBridge;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# direct methods
.method constructor <init>()V
    .locals 0

    .line 152
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public onServiceConnected(Landroid/content/ComponentName;Landroid/os/IBinder;)V
    .locals 2

    .line 156
    :try_start_0
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->access$000()Ljava/lang/Object;

    move-result-object p1

    monitor-enter p1
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    .line 157
    :try_start_1
    invoke-static {p2}, Lcom/allgo/carplay/service/ClusterNavBridge;->access$102(Landroid/os/IBinder;)Landroid/os/IBinder;

    .line 158
    const-string p2, "RHD_CLUSTER"

    const-string v0, "adapter service connected"

    invoke-static {p2, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 159
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->access$200()V

    .line 160
    monitor-exit p1

    .line 163
    goto :goto_0

    .line 160
    :catchall_0
    move-exception p2

    monitor-exit p1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    throw p2
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    .line 161
    :catchall_1
    move-exception p1

    .line 162
    const-string p2, "RHD_CLUSTER"

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "connected failed: "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p2, p1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 164
    :goto_0
    return-void
.end method

.method public onServiceDisconnected(Landroid/content/ComponentName;)V
    .locals 1

    .line 168
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->access$000()Ljava/lang/Object;

    move-result-object p1

    monitor-enter p1

    .line 169
    const/4 v0, 0x0

    :try_start_0
    invoke-static {v0}, Lcom/allgo/carplay/service/ClusterNavBridge;->access$102(Landroid/os/IBinder;)Landroid/os/IBinder;

    .line 170
    const/4 v0, 0x0

    invoke-static {v0}, Lcom/allgo/carplay/service/ClusterNavBridge;->access$302(Z)Z

    .line 171
    invoke-static {}, Lcom/allgo/carplay/service/ClusterNavBridge;->access$400()V

    .line 172
    monitor-exit p1

    .line 173
    return-void

    .line 172
    :catchall_0
    move-exception v0

    monitor-exit p1
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw v0
.end method
