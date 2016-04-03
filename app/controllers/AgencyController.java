package controllers;

import play.mvc.Controller;

public abstract class AgencyController extends Controller {
    protected static final int AGENCY_DOWNLOAD_TIMEOUT_MS = 1000 * 60;

    public abstract void updateAgency();
}