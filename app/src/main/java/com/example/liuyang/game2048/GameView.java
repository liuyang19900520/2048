package com.example.liuyang.game2048;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.GridLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuyang on 2016/3/10.
 */
public class GameView extends GridLayout implements View.OnTouchListener {

    // GameView对应矩阵
    private GameItem[][] mGameMatrix;
    // 空格List
    private List<Point> mBlanks;
    // 矩阵行列数
    private int mGameLines;
    // 记录坐标
    private int mStartX, mStartY, mEndX, mEndY;
    // 辅助数组
    private List<Integer> mCalList;
    private int mKeyItemNum = -1;
    // 历史记录数组
    private int[][] mGameMatrixHistory;
    // 历史记录分数
    private int mScoreHistory;
    // 最高记录
    private int mHighScore;
    // 目标分数
    private int mTarget;

    public GameView(Context context) {
        super(context);
        mTarget = Config.mSp.getInt(Config.KEY_GAME_GOAL, 2048);
        initGameMatrix();
    }

    private void initGameMatrix() {
        // 初始化矩阵
        removeAllViews();
        mScoreHistory = 0;
        Config.SCROE = 0;
        Config.mGameLines = Config.mSp.getInt(Config.KEY_GAME_LINES, 4);
        mGameLines = Config.mGameLines;
        //初始化矩阵宽高
        mGameMatrix = new GameItem[mGameLines][mGameLines];
        mGameMatrixHistory = new int[mGameLines][mGameLines];
        mCalList = new ArrayList<Integer>();
        mBlanks = new ArrayList<Point>();
        mHighScore = Config.mSp.getInt(Config.KEY_HIGH_SCROE, 0);
        setColumnCount(mGameLines);
        setRowCount(mGameLines);
        setOnTouchListener(this);

        //初始化View参数
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(metrics);
        //设置每一个item的宽高
        Config.mItemSize = metrics.widthPixels / Config.mGameLines;

        initGameView(Config.mItemSize);

    }

    private void initGameView(int cardSize) {
        removeAllViews();
        GameItem card;

        for (int i = 0; i < mGameLines; i++) {
            for (int j = 0; j < mGameLines; j++) {
                card = new GameItem(getContext(), 0);
                addView(card, cardSize, cardSize);
                // 初始化GameMatrix全部为0，空格list为所有
                mGameMatrix[i][j] = card;
                mBlanks.add(new Point(i, j));
            }
        }

        addRandomNum();
        addRandomNum();
    }

    /**
     * 启动游戏
     */
    public void startGame() {
        Game.getGameActivity().setScore(0, 0);
        initGameMatrix();
        initGameView(Config.mItemSize);

    }


    /**
     * 添加随机数值
     */
    private void addRandomNum() {
        getBlanks();
        if (mBlanks.size() > 0) {
            int randomNum = (int) (Math.random() * mBlanks.size());
            Point randomPoint = mBlanks.get(randomNum);
            mGameMatrix[randomPoint.x][randomPoint.y].setNum(Math.random() > 0.2d ? 2 : 4);
        }
    }

    /**
     * 获取空格Item数组
     */
    private void getBlanks() {
        mBlanks.clear();
        for (int i = 0; i < mGameLines; i++) {
            for (int j = 0; j < mGameLines; j++) {
                if (mGameMatrix[i][j].getNum() == 0) {
                    mBlanks.add(new Point(i, j));
                }
            }
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //保存历史矩阵
                saveHistoryMatrix();
                mStartX = (int) event.getX();
                mStartY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                mEndX = (int) event.getX();
                mEndY = (int) event.getY();
                judgeDirection(mEndX - mStartX, mEndY - mStartY);
                if (isMoved()) {
                    addRandomNum();
                    Game.getGameActivity().setScore(Config.SCROE, 0);
                }
                checkCompleted();
                break;
            default:
                break;
        }
        return true;
    }

    /*
    * 根据偏移量判断移动方向
     */
    private void judgeDirection(int offsetX, int offsetY) {

        int density = getDeviceDensity();
        int slideDis = 5 * density;
        int maxDis = 250 * density;
        //移动距离大于晃动距离且小于最大距离
        boolean flagNormal = (Math.abs(offsetX) > slideDis || Math.abs(offsetY) > slideDis) &&
                Math.abs(offsetX) < maxDis && Math.abs(offsetY) < maxDis;
        boolean flagSuper = Math.abs(offsetX) > maxDis || Math.abs(offsetY) > maxDis;
        if (flagNormal && !flagSuper) {
            //横向移动
            if (Math.abs(offsetX) > Math.abs(offsetY)) {
                if (offsetX > slideDis) {
                    swipeRight();
                } else {
                    swipeLeft();
                }
                //纵向移动
            } else {
                if (offsetY > slideDis) {
                    swipeDown();
                } else {
                    swipeUp();
                }
            }
        } else if (flagSuper) { // 启动超级用户权限来添加自定义数字
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getContext());
            final EditText et = new EditText(getContext());
            builder.setTitle("Back Door")
                    .setView(et)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    if (!TextUtils.isEmpty(et.getText())) {
                                        addSuperNum(Integer.parseInt(et
                                                .getText().toString()));
                                        checkCompleted();
                                    }
                                }
                            })
                    .setNegativeButton("ByeBye",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    arg0.dismiss();
                                }
                            }).create().show();
        }
    }

    /**
     * 获取屏幕密度
     *
     * @return
     */
    private int getDeviceDensity() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return (int) metrics.density;
    }

    /**
     * 保存历史记录
     */
    private void saveHistoryMatrix() {
        mScoreHistory = Config.SCROE;
        for (int i = 0; i < mGameLines; i++) {
            for (int j = 0; j < mGameLines; j++) {
                mGameMatrixHistory[i][j] = mGameMatrix[i][j].getNum();
            }
        }
    }


    /**
     * 判断是否移动过，是否需要新增item
     *
     * @return
     */
    private boolean isMoved() {

        for (int i = 0; i < mGameLines; i++) {
            for (int j = 0; j < mGameLines; j++) {
                //如果历史记录点的数值和想在点的数值不相等
                if (mGameMatrixHistory[i][j] != mGameMatrix[i][j].getNum()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 撤销上次移动
     */
    public void revertGame() {
        //第一次不能撤销
        int sum = 0;
        //遍历上一次所有的点
        for (int[] element : mGameMatrixHistory) {
            for (int i : element) {
                sum += i;
            }
        }

        if (sum != 0) {
            Game.getGameActivity().setScore(mScoreHistory, 0);
            Config.SCROE = mScoreHistory;
            for (int i = 0; i < mGameLines; i++) {
                for (int j = 0; j < mGameLines; j++) {
                    mGameMatrix[i][j].setNum(mGameMatrixHistory[i][j]);
                }
            }
        }
    }

    /**
     * 滑动事件：右
     */
    private void swipeRight() {
        for (int i = mGameLines - 1; i >= 0; i--) {
            for (int j = mGameLines - 1; j >= 0; j--) {
                int currentNum = mGameMatrix[i][j].getNum();
                if (currentNum != 0) {
                    if (mKeyItemNum == -1) {
                        mKeyItemNum = currentNum;
                    } else {
                        if (mKeyItemNum == currentNum) {
                            mCalList.add(mKeyItemNum * 2);
                            Config.SCROE += mKeyItemNum * 2;
                            mKeyItemNum = -1;
                        } else {
                            mCalList.add(mKeyItemNum);
                            mKeyItemNum = currentNum;
                        }
                    }
                } else {
                    continue;
                }
            }
            if (mKeyItemNum != -1) {
                mCalList.add(mKeyItemNum);
            }
            // 改变Item值
            for (int j = 0; j < mGameLines - mCalList.size(); j++) {
                mGameMatrix[i][j].setNum(0);
            }
            int index = mCalList.size() - 1;
            for (int m = mGameLines - mCalList.size(); m < mGameLines; m++) {
                mGameMatrix[i][m].setNum(mCalList.get(index));
                index--;
            }
            // 重置行参数
            mKeyItemNum = -1;
            mCalList.clear();
            index = 0;
        }
    }

    /**
     * 左滑
     * <p>
     * 通过外层循环遍历每一行，再通过内从循环遍历每一列，对相同的数字进行合并，病通过mKeyItemNum标识来区分是否已经进行过一次合并，例如2 2 0 4，2和2合并后，不会再和4进行合并
     */

    private void swipeLeft() {
        for (int i = 0; i < mGameLines; i++) {
            for (int j = 0; j < mGameLines; j++) {
                //取得当前数
                int currentNum = mGameMatrix[i][j].getNum();
                //当前数不为0,移动的这个数不是空
                if (currentNum != 0) {
                    //mkeyItemNum初始化时,即没有遇到元素时，将其赋值为当前数
                    if (mKeyItemNum == -1) {
                        mKeyItemNum = currentNum;
                        //当遇到的数值不为空时，即直接遇到了一个在的数字
                    } else {
                        //两个数相等
                        if (mKeyItemNum == currentNum) {
                            //像list中添加合并之后的数值
                            mCalList.add(mKeyItemNum * 2);
                            Config.SCROE += mKeyItemNum * 2;
                            //合并之后不会在继续和并 不出出现2 2 4 ===》8的情况
                            mKeyItemNum = -1;
                            //两个数不相等
                        } else {
                            //当遇到的数值不等于当前数
                            mCalList.add(mKeyItemNum);
                            //下一次碰到的数值变为了这次移动端数值
                            mKeyItemNum = currentNum;
                        }
                    }
                } else {
                    //跳出循环，去找下一个有值的数
                    continue;
                }
            }
            if (mKeyItemNum != -1) {
                mCalList.add(mKeyItemNum);
            }

            //改变Item的值
            for (int j = 0; j < mCalList.size(); j++) {
                mGameMatrix[i][j].setNum(mCalList.get(j));
            }
            for (int m = mCalList.size(); m < mGameLines; m++) {
                mGameMatrix[i][m].setNum(0);
            }

            mKeyItemNum = -1;
            mCalList.clear();
        }
    }

    /**
     * 滑动事件：上
     */
    private void swipeUp() {
        for (int i = 0; i < mGameLines; i++) {
            for (int j = 0; j < mGameLines; j++) {
                int currentNum = mGameMatrix[j][i].getNum();
                if (currentNum != 0) {
                    if (mKeyItemNum == -1) {
                        mKeyItemNum = currentNum;
                    } else {
                        if (mKeyItemNum == currentNum) {
                            mCalList.add(mKeyItemNum * 2);
                            Config.SCROE += mKeyItemNum * 2;
                            mKeyItemNum = -1;
                        } else {
                            mCalList.add(mKeyItemNum);
                            mKeyItemNum = currentNum;
                        }
                    }
                } else {
                    continue;
                }
            }
            if (mKeyItemNum != -1) {
                mCalList.add(mKeyItemNum);
            }
            // 改变Item值
            for (int j = 0; j < mCalList.size(); j++) {
                mGameMatrix[j][i].setNum(mCalList.get(j));
            }
            for (int m = mCalList.size(); m < mGameLines; m++) {
                mGameMatrix[m][i].setNum(0);
            }
            // 重置行参数
            mKeyItemNum = -1;
            mCalList.clear();
        }
    }

    /**
     * 滑动事件：下
     */
    private void swipeDown() {
        for (int i = mGameLines - 1; i >= 0; i--) {
            for (int j = mGameLines - 1; j >= 0; j--) {
                int currentNum = mGameMatrix[j][i].getNum();
                if (currentNum != 0) {
                    if (mKeyItemNum == -1) {
                        mKeyItemNum = currentNum;
                    } else {
                        if (mKeyItemNum == currentNum) {
                            mCalList.add(mKeyItemNum * 2);
                            Config.SCROE += mKeyItemNum * 2;
                            mKeyItemNum = -1;
                        } else {
                            mCalList.add(mKeyItemNum);
                            mKeyItemNum = currentNum;
                        }
                    }
                } else {
                    continue;
                }
            }
            if (mKeyItemNum != -1) {
                mCalList.add(mKeyItemNum);
            }
            // 改变Item值
            for (int j = 0; j < mGameLines - mCalList.size(); j++) {
                mGameMatrix[j][i].setNum(0);
            }
            int index = mCalList.size() - 1;
            for (int m = mGameLines - mCalList.size(); m < mGameLines; m++) {
                mGameMatrix[m][i].setNum(mCalList.get(index));
                index--;
            }
            // 重置行参数
            mKeyItemNum = -1;
            mCalList.clear();
            index = 0;
        }
    }


    /**
     * super模式下添加一个指定数字
     */
    private void addSuperNum(int num) {
        if (checkSuperNum(num)) {
            getBlanks();
            if (mBlanks.size() > 0) {
                int randomNum = (int) (Math.random() * mBlanks.size());
                Point randomPoint = mBlanks.get(randomNum);
                mGameMatrix[randomPoint.x][randomPoint.y].setNum(num);
                animCreate(mGameMatrix[randomPoint.x][randomPoint.y]);
            }
        }
    }

    /**
     * 检查添加的数是否是指定的数
     *
     * @param num num
     * @return 添加的数
     */
    private boolean checkSuperNum(int num) {
        boolean flag = (num == 2 || num == 4 || num == 8 || num == 16
                || num == 32 || num == 64 || num == 128 || num == 256
                || num == 512 || num == 1024);
        return flag;
    }

    /**
     * 生成动画
     *
     * @param target GameItem
     */
    private void animCreate(GameItem target) {
        ScaleAnimation sa = new ScaleAnimation(0.1f, 1, 0.1f, 1,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        sa.setDuration(100);
        target.setAnimation(null);
        target.getItemView().startAnimation(sa);
    }


    /**
     * 判断是否结束
     * <p>
     * 0:结束 1:正常 2:成功
     */
    private void checkCompleted() {
        int result = checkNums();
        if (result == 0) {
            if (Config.SCROE > mHighScore) {
                SharedPreferences.Editor editor = Config.mSp.edit();
                editor.putInt(Config.KEY_HIGH_SCROE, Config.SCROE);
                editor.apply();
                Game.getGameActivity().setScore(Config.SCROE, 1);
                Config.SCROE = 0;
            }
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            builder.setTitle("Game Over")
                    .setPositiveButton("Again",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    startGame();
                                }
                            }).create().show();
            Config.SCROE = 0;
        } else if (result == 2) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            builder.setTitle("Mission Accomplished")
                    .setPositiveButton("Again",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    // 重新开始
                                    startGame();
                                }
                            })
                    .setNegativeButton("Continue",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    // 继续游戏 修改target
                                    SharedPreferences.Editor editor = Config.mSp.edit();
                                    if (mTarget == 1024) {
                                        editor.putInt(Config.KEY_GAME_GOAL, 2048);
                                        mTarget = 2048;
                                        Game.getGameActivity().setGoal(2048);
                                    } else if (mTarget == 2048) {
                                        editor.putInt(Config.KEY_GAME_GOAL, 4096);
                                        mTarget = 4096;
                                        Game.getGameActivity().setGoal(4096);
                                    } else {
                                        editor.putInt(Config.KEY_GAME_GOAL, 4096);
                                        mTarget = 4096;
                                        Game.getGameActivity().setGoal(4096);
                                    }
                                    editor.apply();
                                }
                            }).create().show();
            Config.SCROE = 0;
        }
    }


    /**
     * 检测所有数字
     *
     * @return 0 结束 1 正常 2 成功
     */

    private int checkNums() {
        getBlanks();
        if (mBlanks.size() == 0) {
            for (int i = 0; i < mGameLines; i++) {
                for (int j = 0; j < mGameLines; j++) {
                    if (j < mGameLines - 1) {
                        if (mGameMatrix[i][j].getNum() == mGameMatrix[i][j + 1].getNum()) {
                            return 1;
                        }
                    }
                    if (i < mGameLines - 1) {
                        if (mGameMatrix[i][j].getNum() == mGameMatrix[i + 1][j].getNum()) {
                            return 1;
                        }
                    }
                }
            }
            return 0;
        }
        for (int i = 0; i < mGameLines; i++) {
            for (int j = 0; j < mGameLines; j++) {
                if (mGameMatrix[i][j].getNum() == mTarget) {
                    return 2;
                }
            }
        }
        return 1;
    }


}
